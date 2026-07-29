package orga.takwa.ummati.service;

import orga.takwa.ummati.entity.NotificationPreferences;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.NotificationCategory;
import orga.takwa.ummati.entity.enums.NotificationType;
import orga.takwa.ummati.repository.NotificationPreferencesRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Préférences d'email : ce qui peut être coupé, et ce qui doit passer quoi qu'il arrive.
 */
@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock private NotificationPreferencesRepository preferencesRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private NotificationPreferenceService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("amina@test.com");
        user.setEnabled(true);
        user.setEmailVerified(true);
        lenient().when(preferencesRepository.save(any(NotificationPreferences.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private void givenPreferences(NotificationPreferences prefs) {
        when(preferencesRepository.findById(user.getId())).thenReturn(Optional.ofNullable(prefs));
    }

    // --- Classement des types ---

    @Test
    void alertsFollowsAndFavoriteRemindersAreSolicitations() {
        assertThat(NotificationCategory.of(NotificationType.MISSION_ALERT))
                .isEqualTo(NotificationCategory.NEW_MISSIONS);
        assertThat(NotificationCategory.of(NotificationType.ORG_NEW_EVENT))
                .isEqualTo(NotificationCategory.NEW_MISSIONS);
        assertThat(NotificationCategory.of(NotificationType.FAVORITE_CLOSING))
                .isEqualTo(NotificationCategory.NEW_MISSIONS);
    }

    @Test
    void signupAndCancellationStayTransactional() {
        // Le bénévole a une place réservée : il doit être prévenu même s'il a tout coupé.
        assertThat(NotificationCategory.of(NotificationType.SIGNUP_CONFIRMED))
                .isEqualTo(NotificationCategory.TRANSACTIONAL);
        assertThat(NotificationCategory.of(NotificationType.EVENT_CANCELLED))
                .isEqualTo(NotificationCategory.TRANSACTIONAL);
        assertThat(NotificationCategory.of(NotificationType.SIGNUP_PROMOTED))
                .isEqualTo(NotificationCategory.TRANSACTIONAL);
    }

    // --- Filtrage à l'envoi ---

    @Test
    void shouldAllowEverythingByDefault() {
        givenPreferences(null);

        assertThat(service.allowsEmail(user, NotificationCategory.NEW_MISSIONS)).isTrue();
        assertThat(service.allowsEmail(user, NotificationCategory.REMINDERS)).isTrue();
    }

    @Test
    void shouldBlockADisabledCategory() {
        NotificationPreferences prefs = new NotificationPreferences();
        prefs.setEmailNewMissions(false);
        givenPreferences(prefs);

        assertThat(service.allowsEmail(user, NotificationCategory.NEW_MISSIONS)).isFalse();
        assertThat(service.allowsEmail(user, NotificationCategory.REMINDERS)).isTrue();
    }

    @Test
    void shouldNeverBlockTransactionalEmails() {
        NotificationPreferences prefs = new NotificationPreferences();
        prefs.setEmailNewMissions(false);
        prefs.setEmailReminders(false);
        prefs.setEmailMemberships(false);
        prefs.setEmailAnnouncements(false);

        // Aucun accès au dépôt n'est même nécessaire : le transactionnel court-circuite.
        assertThat(service.allowsEmail(user, NotificationCategory.TRANSACTIONAL)).isTrue();
    }

    @Test
    void shouldNotSolicitAnUnverifiedAccount() {
        user.setEmailVerified(false);

        assertThat(service.allowsEmail(user, NotificationCategory.NEW_MISSIONS)).isFalse();
    }

    @Test
    void shouldNotSolicitADisabledAccount() {
        user.setEnabled(false);

        assertThat(service.allowsEmail(user, NotificationCategory.NEW_MISSIONS)).isFalse();
    }

    // --- Désinscription en un clic ---

    @Test
    void unsubscribeWithoutCategoryShouldCutEverySolicitation() {
        NotificationPreferences prefs = new NotificationPreferences();
        user.setUnsubscribeToken("jeton");
        when(userRepository.findByUnsubscribeToken("jeton")).thenReturn(Optional.of(user));
        givenPreferences(prefs);

        assertThat(service.unsubscribeByToken("jeton", null)).isTrue();

        assertThat(prefs.isEmailNewMissions()).isFalse();
        assertThat(prefs.isEmailReminders()).isFalse();
        assertThat(prefs.isEmailMemberships()).isFalse();
        assertThat(prefs.isEmailAnnouncements()).isFalse();
    }

    @Test
    void unsubscribeWithCategoryShouldCutOnlyThatOne() {
        NotificationPreferences prefs = new NotificationPreferences();
        when(userRepository.findByUnsubscribeToken("jeton")).thenReturn(Optional.of(user));
        givenPreferences(prefs);

        service.unsubscribeByToken("jeton", NotificationCategory.NEW_MISSIONS);

        assertThat(prefs.isEmailNewMissions()).isFalse();
        assertThat(prefs.isEmailReminders()).isTrue();
    }

    @Test
    void unsubscribeShouldStaySilentOnAnUnknownToken() {
        // Répondre différemment ferait du lien un moyen de tester des jetons.
        when(userRepository.findByUnsubscribeToken("inconnu")).thenReturn(Optional.empty());

        assertThat(service.unsubscribeByToken("inconnu", null)).isFalse();
        assertThat(service.unsubscribeByToken(null, null)).isFalse();
        assertThat(service.unsubscribeByToken("  ", null)).isFalse();
    }

    @Test
    void shouldGenerateAStableUnsubscribeToken() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String first = service.unsubscribeTokenFor(user);
        String second = service.unsubscribeTokenFor(user);

        assertThat(first).isNotBlank().hasSize(48);
        assertThat(second).isEqualTo(first);
    }
}
