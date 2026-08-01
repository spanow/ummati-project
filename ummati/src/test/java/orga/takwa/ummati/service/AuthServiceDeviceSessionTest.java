package orga.takwa.ummati.service;

import orga.takwa.ummati.config.security.JwtTokenProvider;
import orga.takwa.ummati.dto.auth.AuthResponse;
import orga.takwa.ummati.dto.auth.DeviceContext;
import orga.takwa.ummati.dto.auth.LoginRequest;
import orga.takwa.ummati.dto.auth.TokenRefreshResponse;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.DevicePlatform;
import orga.takwa.ummati.entity.enums.UserRole;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.repository.UserRepository;
import orga.takwa.ummati.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Cohabitation des deux régimes de session.
 *
 * <p>L'app installée obtient une session longue et révocable, tandis que le client
 * web conserve exactement le refresh JWT sans état d'avant. Ces tests existent
 * surtout pour la seconde moitié de la phrase : le déploiement du socle mobile ne
 * doit déconnecter aucune session web ouverte.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceDeviceSessionTest {

    @Mock private UserRepository userRepository;
    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private EmailService emailService;
    @Mock private RefreshTokenService refreshTokenService;

    private AuthService authService;
    private User user;

    private static final String JWT_REFRESH = "jwt.refresh.stateless";
    private static final String OPAQUE_REFRESH = "opaque-native-token";

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, tokenRepository, notificationService,
                auditService, passwordEncoder, jwtTokenProvider, emailService, refreshTokenService);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("benevole@ummati.org");
        user.setPasswordHash("hash");
        user.setRole(UserRole.VOLUNTEER);
        user.setEnabled(true);
        user.setEmailVerified(true);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900_000L);
    }

    @Nested
    @DisplayName("Connexion")
    class Login {

        @Test
        @DisplayName("Un client web reçoit toujours le refresh JWT sans état")
        void webLoginKeepsStatelessJwt() {
            when(jwtTokenProvider.generateRefreshToken(any())).thenReturn(JWT_REFRESH);

            AuthResponse response = authService.login(
                    new LoginRequest("benevole@ummati.org", "pwd"), "127.0.0.1");

            assertThat(response.refreshToken()).isEqualTo(JWT_REFRESH);
            verify(refreshTokenService, never()).issue(any(), any());
        }

        @Test
        @DisplayName("Une app installée reçoit une session longue stockée")
        void nativeLoginIssuesStoredSession() {
            when(refreshTokenService.issue(any(), any())).thenReturn(
                    new RefreshTokenService.IssuedToken(OPAQUE_REFRESH, null));

            AuthResponse response = authService.login(
                    new LoginRequest("benevole@ummati.org", "pwd"), "127.0.0.1",
                    new DeviceContext(DevicePlatform.IOS, "device-1", "iPhone", "1.0.0"));

            assertThat(response.refreshToken()).isEqualTo(OPAQUE_REFRESH);
            verify(jwtTokenProvider, never()).generateRefreshToken(any());
        }
    }

    @Nested
    @DisplayName("Rafraîchissement")
    class Refresh {

        @Test
        @DisplayName("Un refresh JWT web déjà émis continue de fonctionner, sans rotation")
        void statelessJwtStillWorks() {
            when(refreshTokenService.rotate(any())).thenReturn(Optional.empty());
            when(jwtTokenProvider.validateToken(JWT_REFRESH)).thenReturn(true);
            when(jwtTokenProvider.isRefreshToken(JWT_REFRESH)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(JWT_REFRESH)).thenReturn(user.getId());

            TokenRefreshResponse response = authService.refreshToken(JWT_REFRESH);

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken())
                    .as("le régime web n'a pas de rotation : rien à renvoyer")
                    .isNull();
        }

        @Test
        @DisplayName("Une session native reçoit son jeton de remplacement")
        void nativeSessionIsRotated() {
            when(refreshTokenService.rotate(OPAQUE_REFRESH)).thenReturn(Optional.of(
                    new RefreshTokenService.RotationResult(user, "opaque-next", null)));

            TokenRefreshResponse response = authService.refreshToken(OPAQUE_REFRESH);

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("opaque-next");
            verify(jwtTokenProvider, never()).validateToken(any());
        }

        @Test
        @DisplayName("Un compte désactivé ne peut pas rafraîchir sa session native")
        void disabledAccountCannotRefreshNativeSession() {
            user.setEnabled(false);
            when(refreshTokenService.rotate(OPAQUE_REFRESH)).thenReturn(Optional.of(
                    new RefreshTokenService.RotationResult(user, "opaque-next", null)));

            assertThatThrownBy(() -> authService.refreshToken(OPAQUE_REFRESH))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("Déconnexion")
    class Logout {

        @Test
        @DisplayName("La session présentée est révoquée côté serveur")
        void logoutRevokesSession() {
            authService.logout(OPAQUE_REFRESH);

            verify(refreshTokenService).revoke(OPAQUE_REFRESH);
        }
    }
}
