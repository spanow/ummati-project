package orga.takwa.ummati.service;

import orga.takwa.ummati.entity.*;
import orga.takwa.ummati.entity.enums.*;
import orga.takwa.ummati.repository.EventOccurrenceRepository;
import orga.takwa.ummati.repository.EventPhotoRepository;
import orga.takwa.ummati.repository.EventRepository;
import orga.takwa.ummati.repository.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Relance « ajoutez des photos » déclenchée par la clôture automatique d'une mission.
 */
@ExtendWith(MockitoExtension.class)
class EventCompletionJobPhotosTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventOccurrenceRepository occurrenceRepository;
    @Mock private EventPhotoRepository eventPhotoRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private EventCompletionJob job;

    private Event event;
    private Organization org;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        org = new Organization();
        org.setId(UUID.randomUUID());
        org.setName("Croix-Rouge");
        org.setSlug("croix-rouge");

        event = new Event();
        event.setId(eventId);
        event.setTitle("Maraude du samedi");
        event.setOrganization(org);
        event.setStatus(EventStatus.PUBLISHED);
    }

    /** Un créneau publié dont la fin est passée, seul créneau de la série. */
    private void givenExpiredOccurrence() {
        EventOccurrence occ = new EventOccurrence();
        occ.setEvent(event);
        occ.setStatus(EventOccurrenceStatus.PUBLISHED);
        when(occurrenceRepository.findByStatusAndEndDateBefore(eq(EventOccurrenceStatus.PUBLISHED), any()))
                .thenReturn(List.of(occ));
        when(occurrenceRepository.countByEventIdAndStatus(eventId, EventOccurrenceStatus.PUBLISHED))
                .thenReturn(0L);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
    }

    private Membership admin(String firstName) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName(firstName);
        Membership m = new Membership();
        m.setUser(user);
        m.setOrganization(org);
        m.setRole(MembershipRole.ADMIN);
        m.setStatus(MembershipStatus.ACTIVE);
        return m;
    }

    @Test
    void shouldNotifyEveryOrgAdmin_whenGalleryIsEmpty() {
        givenExpiredOccurrence();
        when(eventPhotoRepository.countByEventId(eventId)).thenReturn(0L);
        when(membershipRepository.findByOrganizationIdAndRoleAndStatus(
                org.getId(), MembershipRole.ADMIN, MembershipStatus.ACTIVE))
                .thenReturn(List.of(admin("Amina"), admin("Karim")));

        job.completeExpiredEvents();

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(2)).saveNotification(
                any(User.class), eq(NotificationType.EVENT_PHOTOS_REQUESTED),
                title.capture(), anyString(), anyString());
        assertThat(title.getValue()).contains("Maraude du samedi");
    }

    @Test
    void shouldStaySilent_whenGalleryAlreadyHasPhotos() {
        givenExpiredOccurrence();
        when(eventPhotoRepository.countByEventId(eventId)).thenReturn(3L);

        job.completeExpiredEvents();

        verifyNoInteractions(notificationService);
        verify(membershipRepository, never()).findByOrganizationIdAndRoleAndStatus(any(), any(), any());
    }

    @Test
    void shouldLinkToTheEventManagementScreen_whereVisualsAreEdited() {
        givenExpiredOccurrence();
        when(eventPhotoRepository.countByEventId(eventId)).thenReturn(0L);
        when(membershipRepository.findByOrganizationIdAndRoleAndStatus(any(), any(), any()))
                .thenReturn(List.of(admin("Amina")));

        job.completeExpiredEvents();

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(notificationService).saveNotification(
                any(User.class), any(), anyString(), anyString(), link.capture());
        assertThat(link.getValue()).isEqualTo("/organizations/" + org.getId() + "/events/manage");
    }

    @Test
    void shouldNotNotify_whenEventWasAlreadyCompleted() {
        // Statut déjà COMPLETED : la transition n'a pas lieu, donc pas de relance.
        event.setStatus(EventStatus.COMPLETED);
        givenExpiredOccurrence();

        job.completeExpiredEvents();

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotNotify_whenTheSeriesStillHasPublishedOccurrences() {
        EventOccurrence occ = new EventOccurrence();
        occ.setEvent(event);
        occ.setStatus(EventOccurrenceStatus.PUBLISHED);
        occ.setEndDate(LocalDateTime.now().minusDays(1));
        when(occurrenceRepository.findByStatusAndEndDateBefore(eq(EventOccurrenceStatus.PUBLISHED), any()))
                .thenReturn(List.of(occ));
        // Il reste un créneau à venir : la série n'est pas terminée.
        when(occurrenceRepository.countByEventIdAndStatus(eventId, EventOccurrenceStatus.PUBLISHED))
                .thenReturn(2L);

        job.completeExpiredEvents();

        verifyNoInteractions(notificationService);
        verify(eventRepository, never()).save(any());
    }
}
