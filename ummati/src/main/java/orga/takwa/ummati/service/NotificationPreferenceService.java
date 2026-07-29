package orga.takwa.ummati.service;

import orga.takwa.ummati.entity.NotificationPreferences;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.NotificationCategory;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.NotificationPreferencesRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Préférences d'envoi d'emails, et désinscription en un clic.
 *
 * <p>Toute la plateforme passe par {@link #allowsEmail} avant d'envoyer un email non
 * transactionnel. Sans ce point de passage, chaque nouvelle source de notification
 * (alertes, ONG suivies, relances) aggraverait le problème : beaucoup d'emails,
 * aucun moyen de les arrêter, et un domaine d'envoi qui finit en indésirable.
 */
@Service
public class NotificationPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPreferenceService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final NotificationPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;

    public NotificationPreferenceService(NotificationPreferencesRepository preferencesRepository,
                                         UserRepository userRepository) {
        this.preferencesRepository = preferencesRepository;
        this.userRepository = userRepository;
    }

    /**
     * Préférences du bénévole, créées au premier accès avec tout activé.
     *
     * <p>Créer à la volée plutôt qu'à l'inscription évite d'avoir à reprendre les
     * comptes existants et garde le service tolérant à une ligne manquante.
     */
    @Transactional
    public NotificationPreferences getOrCreate(User user) {
        return preferencesRepository.findById(user.getId()).orElseGet(() -> {
            NotificationPreferences prefs = new NotificationPreferences();
            prefs.setUser(user);
            return preferencesRepository.save(prefs);
        });
    }

    /** Vrai si un email de cette catégorie peut partir vers ce bénévole. */
    @Transactional
    public boolean allowsEmail(User user, NotificationCategory category) {
        if (category == NotificationCategory.TRANSACTIONAL) {
            return true;
        }
        // Un compte désactivé ou non vérifié ne reçoit aucun email de sollicitation.
        if (!user.isEnabled() || !user.isEmailVerified()) {
            return false;
        }
        return getOrCreate(user).allows(category);
    }

    @Transactional
    public NotificationPreferences update(UUID userId, NotificationCategory category, boolean value) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        NotificationPreferences prefs = getOrCreate(user);
        prefs.set(category, value);
        return preferencesRepository.save(prefs);
    }

    /**
     * Désinscription depuis un email, sans connexion.
     *
     * <p>Silencieuse sur un jeton inconnu : répondre « ce jeton n'existe pas »
     * transformerait le lien en oracle permettant de tester des jetons.
     */
    @Transactional
    public boolean unsubscribeByToken(String token, NotificationCategory category) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return userRepository.findByUnsubscribeToken(token).map(user -> {
            NotificationPreferences prefs = getOrCreate(user);
            if (category == null) {
                // Sans catégorie précisée, on coupe tout ce qui est désactivable.
                prefs.setEmailNewMissions(false);
                prefs.setEmailReminders(false);
                prefs.setEmailMemberships(false);
                prefs.setEmailAnnouncements(false);
            } else {
                prefs.set(category, false);
            }
            preferencesRepository.save(prefs);
            log.info("Désinscription email pour l'utilisateur {} (catégorie {})",
                    user.getId(), category == null ? "toutes" : category);
            return true;
        }).orElse(false);
    }

    /** Jeton de désinscription du bénévole, généré à la première utilisation. */
    @Transactional
    public String unsubscribeTokenFor(User user) {
        if (user.getUnsubscribeToken() != null && !user.getUnsubscribeToken().isBlank()) {
            return user.getUnsubscribeToken();
        }
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        user.setUnsubscribeToken(token);
        userRepository.save(user);
        return token;
    }
}
