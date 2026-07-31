package orga.takwa.ummati.service;

import orga.takwa.ummati.config.security.JwtTokenProvider;
import orga.takwa.ummati.dto.auth.*;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.VerificationToken;
import orga.takwa.ummati.entity.enums.NotificationType;
import orga.takwa.ummati.entity.enums.TokenType;
import orga.takwa.ummati.entity.enums.UserRole;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ConflictException;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.UserRepository;
import orga.takwa.ummati.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, VerificationTokenRepository tokenRepository,
                       NotificationService notificationService, AuditService auditService,
                       PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider,
                       EmailService emailService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
        this.refreshTokenService = refreshTokenService;
    }

    // ===== REGISTER (T-023) =====

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new ConflictException("Un compte avec cet email existe déjà");
        }

        User user = new User();
        user.setEmail(request.email().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setRole(UserRole.VOLUNTEER);
        // Jeton de désinscription posé dès la création : la migration V21 n'a couvert
        // que les comptes existants, et un compte sans jeton ne pourrait pas se
        // désabonner depuis un email tant qu'aucun envoi n'aurait eu lieu.
        user.setUnsubscribeToken(generateUnsubscribeToken());
        user = userRepository.save(user);

        // Verification token (24h)
        String token = createVerificationToken(user, TokenType.EMAIL_VERIFICATION, 24);

        notificationService.saveNotification(user, NotificationType.WELCOME,
                "Bienvenue sur Ummati !", "Votre compte a été créé. Confirmez votre email pour commencer.", "/profile");

        auditService.log(user.getId(), "USER_REGISTERED", "User", user.getId());

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), token);

        return new RegisterResponse(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole().name(), user.isEmailVerified(), user.getCreatedAt()
        );
    }

    // ===== CONFIRM EMAIL (T-024) =====

    @Transactional
    public void confirmEmail(String token) {
        VerificationToken vt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessRuleException("Token invalide"));

        if (vt.isUsed()) {
            throw new ConflictException("Email déjà confirmé");
        }
        if (vt.isExpired()) {
            throw new BusinessRuleException("Token expiré. Demandez un nouvel email de confirmation.");
        }
        if (vt.getType() != TokenType.EMAIL_VERIFICATION) {
            throw new BusinessRuleException("Token invalide");
        }

        vt.setUsedAt(LocalDateTime.now());
        tokenRepository.save(vt);

        User user = vt.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        auditService.log(user.getId(), "EMAIL_VERIFIED", "User", user.getId());
    }

    // ===== RESEND CONFIRMATION (T-025) =====

    @Transactional
    public void resendConfirmation(String email) {
        // Always return 200 (no leak)
        userRepository.findByEmail(email.toLowerCase().trim()).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                tokenRepository.invalidateAllByUserAndType(user.getId(), TokenType.EMAIL_VERIFICATION);
                String token = createVerificationToken(user, TokenType.EMAIL_VERIFICATION, 24);
                emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), token);
            }
        });
    }

    // ===== LOGIN (T-026) =====

    /** Connexion d'un client web : conserve le refresh JWT sans état d'origine. */
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        return login(request, ipAddress, DeviceContext.web());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, DeviceContext device) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BusinessRuleException("Email ou mot de passe incorrect"));

        // Check locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Compte temporairement bloqué. Réessayez plus tard.");
        }

        // Le mot de passe est vérifié AVANT l'état du compte. Dans l'ordre inverse,
        // « Compte désactivé » et « Email non vérifié » se déclenchaient sans connaître
        // le mot de passe : il suffisait d'essayer une adresse pour savoir si elle était
        // inscrite sur la plateforme (énumération de comptes).
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedLogin(user, ipAddress);
            throw new BusinessRuleException("Email ou mot de passe incorrect");
        }

        // Check enabled
        if (!user.isEnabled()) {
            throw new ForbiddenException("Compte désactivé");
        }

        // Check email verified
        if (!user.isEmailVerified()) {
            throw new ForbiddenException("Email non vérifié. Vérifiez votre boîte mail.");
        }

        // Success — reset failed attempts
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());

        // Une app installée reçoit une session longue, révocable et rotative ; un
        // navigateur garde le refresh JWT de 7 jours. Les deux régimes coexistent pour
        // que le déploiement ne déconnecte aucune session web ouverte.
        String refreshToken = device.isNative()
                ? refreshTokenService.issue(user, device).rawToken()
                : jwtTokenProvider.generateRefreshToken(user.getId());

        auditService.log(user.getId(), "LOGIN_SUCCESS", "User", user.getId(), ipAddress);

        return new AuthResponse(
                accessToken, refreshToken,
                jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
                "Bearer",
                new AuthResponse.UserSummary(
                        user.getId().toString(), user.getEmail(),
                        user.getFirstName(), user.getLastName(),
                        user.getRole().name(), user.isOnboardingDone()
                )
        );
    }

    // ===== REFRESH TOKEN (T-027) =====

    /**
     * Rafraîchit un access token, quel que soit le régime du porteur.
     *
     * <p>Les jetons natifs stockés sont examinés en premier : ce sont des chaînes
     * aléatoires que {@code jwtTokenProvider} rejetterait de toute façon. Si la valeur
     * n'appartient pas au registre, on retombe sur le refresh JWT sans état — c'est
     * ce qui préserve les sessions web déjà ouvertes.
     */
    public TokenRefreshResponse refreshToken(String refreshTokenStr) {
        var rotated = refreshTokenService.rotate(refreshTokenStr);
        if (rotated.isPresent()) {
            User user = rotated.get().user();
            if (!user.isEnabled()) {
                throw new ForbiddenException("Compte désactivé");
            }
            return new TokenRefreshResponse(
                    jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name()),
                    jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
                    "Bearer",
                    rotated.get().rawToken()
            );
        }

        if (!jwtTokenProvider.validateToken(refreshTokenStr) || !jwtTokenProvider.isRefreshToken(refreshTokenStr)) {
            throw new BusinessRuleException("Refresh token invalide");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshTokenStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!user.isEnabled()) {
            throw new ForbiddenException("Compte désactivé");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());

        return TokenRefreshResponse.stateless(
                newAccessToken,
                jwtTokenProvider.getAccessTokenExpirationMs() / 1000
        );
    }

    /**
     * Déconnexion : révoque réellement la session native présentée, là où le web se
     * contentait d'oublier ses jetons côté client. Sans cela, une session de 90 jours
     * survivrait à la déconnexion sur un téléphone prêté ou revendu.
     */
    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenService.revoke(refreshTokenStr);
    }

    // ===== FORGOT PASSWORD (T-028) =====

    @Transactional
    public void forgotPassword(String email) {
        // Always 200 — no leak
        userRepository.findByEmail(email.toLowerCase().trim()).ifPresent(user -> {
            if (user.isEmailVerified() && user.isEnabled()) {
                tokenRepository.invalidateAllByUserAndType(user.getId(), TokenType.PASSWORD_RESET);
                String token = createVerificationToken(user, TokenType.PASSWORD_RESET, 1);
                emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);
            }
        });
    }

    // ===== RESET PASSWORD (T-029) =====

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        VerificationToken vt = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BusinessRuleException("Token invalide"));

        if (vt.isUsed()) {
            throw new BusinessRuleException("Token déjà utilisé");
        }
        if (vt.isExpired()) {
            throw new BusinessRuleException("Token expiré. Demandez un nouveau lien.");
        }
        if (vt.getType() != TokenType.PASSWORD_RESET) {
            throw new BusinessRuleException("Token invalide");
        }

        vt.setUsedAt(LocalDateTime.now());
        tokenRepository.save(vt);

        User user = vt.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // Le compteur d'échecs et le blocage temporaire sont remis à zéro : sans cela, un
        // utilisateur bloqué après 5 tentatives restait bloqué 30 minutes de plus alors
        // qu'il vient précisément de prouver son identité par email.
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // Toutes les sessions natives tombent avec l'ancien mot de passe. Sans cela, le
        // scénario que la réinitialisation est censée traiter — un compte compromis —
        // laisserait l'intrus connecté 90 jours sur son propre téléphone.
        refreshTokenService.revokeAllForUser(user.getId());

        auditService.log(user.getId(), "PASSWORD_RESET", "User", user.getId());
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());
    }

    // ===== HELPERS =====

    private void handleFailedLogin(User user, String ipAddress) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("Account locked for user {} after {} failed attempts", user.getEmail(), attempts);
        }

        userRepository.save(user);
        auditService.log(user.getId(), "LOGIN_FAILED", "User", user.getId(), ipAddress);
    }

    private static String generateUnsubscribeToken() {
        byte[] bytes = new byte[24];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.HexFormat.of().formatHex(bytes);
    }

    private String createVerificationToken(User user, TokenType type, int expirationHours) {
        VerificationToken vt = new VerificationToken();
        vt.setUser(user);
        vt.setToken(UUID.randomUUID().toString());
        vt.setType(type);
        vt.setExpiresAt(LocalDateTime.now().plusHours(expirationHours));
        tokenRepository.save(vt);
        return vt.getToken();
    }

}

