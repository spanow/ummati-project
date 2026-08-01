package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.auth.DeviceContext;
import orga.takwa.ummati.entity.RefreshToken;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Sessions longues des clients installés.
 *
 * <p>Un refresh JWT de 7 jours convient à un navigateur ; sur mobile il impose une
 * reconnexion hebdomadaire et n'offre aucun moyen de couper l'accès d'un téléphone
 * perdu. On émet donc pour les apps natives un jeton opaque, stocké sous forme
 * d'empreinte, à durée longue mais tournant à chaque usage.
 *
 * <p>Ce service ne remplace rien : le web continue d'utiliser le refresh JWT sans
 * état. Les deux régimes cohabitent, {@code AuthService} choisit selon le
 * {@link DeviceContext}.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long validityDays;

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${app.jwt.native-refresh-token-expiration-days:90}") long validityDays) {
        this.repository = repository;
        this.validityDays = validityDays;
    }

    /** Jeton en clair (jamais persisté) et son terme, à renvoyer au client. */
    public record IssuedToken(String rawToken, LocalDateTime expiresAt) {}

    /** Résultat d'une rotation réussie : le porteur et son jeton de remplacement. */
    public record RotationResult(User user, String rawToken, LocalDateTime expiresAt) {}

    @Transactional
    public IssuedToken issue(User user, DeviceContext device) {
        return issue(user, device, UUID.randomUUID());
    }

    /**
     * Échange un jeton contre son successeur.
     *
     * <p>Renvoie {@code Optional.empty()} lorsque la valeur présentée n'appartient pas
     * à ce mécanisme : l'appelant doit alors essayer le refresh JWT historique. C'est
     * ce qui permet aux sessions web déjà ouvertes de survivre au déploiement.
     *
     * @throws ForbiddenException si le jeton est révoqué, expiré, ou rejoué
     */
    @Transactional
    public Optional<RotationResult> rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();

        Optional<RefreshToken> found = repository.findByTokenHash(hash(rawToken));
        if (found.isEmpty()) return Optional.empty();

        RefreshToken token = found.get();

        // Un jeton déjà consommé qui resurgit ne peut pas venir du client légitime :
        // celui-ci détient le dernier maillon de la chaîne. On considère la lignée
        // compromise et on la coupe entièrement, ce qui déconnecte aussi le voleur.
        if (token.getUsedAt() != null) {
            int revoked = repository.revokeFamily(token.getFamilyId(), LocalDateTime.now());
            log.warn("Rejeu d'un refresh token détecté pour l'utilisateur {} — {} session(s) révoquée(s)",
                    token.getUser().getId(), revoked);
            throw new ForbiddenException("Session révoquée. Reconnectez-vous.");
        }

        if (!token.isUsable()) {
            throw new ForbiddenException("Session expirée. Reconnectez-vous.");
        }

        token.setUsedAt(LocalDateTime.now());
        repository.save(token);

        User user = token.getUser();
        DeviceContext device = new DeviceContext(
                token.getPlatform(), token.getDeviceId(), token.getDeviceName(), null);
        IssuedToken next = issue(user, device, token.getFamilyId());

        return Optional.of(new RotationResult(user, next.rawToken(), next.expiresAt()));
    }

    /**
     * Révoque la session portant ce jeton, et avec elle toute sa lignée de rotations.
     * Silencieux si le jeton est inconnu : une déconnexion ne doit jamais échouer.
     */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> repository.revokeFamily(token.getFamilyId(), LocalDateTime.now()));
    }

    /** Déconnexion de tous les appareils — changement de mot de passe, compte compromis. */
    @Transactional
    public int revokeAllForUser(UUID userId) {
        return repository.revokeAllForUser(userId, LocalDateTime.now());
    }

    private IssuedToken issue(User user, DeviceContext device, UUID familyId) {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(validityDays);

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hash(rawToken));
        entity.setFamilyId(familyId);
        entity.setPlatform(device.platform());
        entity.setDeviceId(device.deviceId());
        entity.setDeviceName(device.deviceName());
        entity.setExpiresAt(expiresAt);
        repository.save(entity);

        return new IssuedToken(rawToken, expiresAt);
    }

    /**
     * SHA-256 sans sel : l'entrée est déjà 256 bits d'aléa cryptographique, un sel ou
     * un KDF lent n'ajouterait rien face à une recherche exhaustive impossible — et
     * cette empreinte est calculée à chaque rafraîchissement.
     */
    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
