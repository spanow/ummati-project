package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.alert.MissionAlertRequest;
import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.MissionAlert;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.*;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.repository.EventRepository;
import orga.takwa.ummati.repository.MissionAlertRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionAlertServiceTest {

    @Mock private MissionAlertRepository alertRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private MissionAlertService service;

    private User user;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        now = LocalDateTime.of(2026, 8, 1, 8, 0);
        lenient().when(alertRepository.save(any(MissionAlert.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    private MissionAlert alert(String city, Double lat, Double lng, Integer radius,
                               String domains, String types) {
        MissionAlert a = new MissionAlert();
        a.setUser(user);
        a.setLabel("Mon alerte");
        a.setCity(city);
        if (lat != null) a.setLat(BigDecimal.valueOf(lat));
        if (lng != null) a.setLng(BigDecimal.valueOf(lng));
        a.setRadiusKm(radius);
        a.setDomains(domains);
        a.setTypes(types);
        a.setFrequency(AlertFrequency.WEEKLY);
        a.setLastSentAt(now.minusDays(7));
        return a;
    }

    private Event event(String title, String city, Double lat, Double lng,
                        EventType type, OrganizationDomain domain) {
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setName("ONG");
        org.setDomain(domain);

        Event e = new Event();
        e.setId(UUID.randomUUID());
        e.setTitle(title);
        e.setOrganization(org);
        e.setType(type);
        e.setStatus(EventStatus.PUBLISHED);
        e.setLocationCity(city);
        if (lat != null) e.setLocationLat(BigDecimal.valueOf(lat));
        if (lng != null) e.setLocationLng(BigDecimal.valueOf(lng));
        e.setStartDate(now.plusDays(10));
        return e;
    }

    private void givenCandidates(Event... events) {
        when(eventRepository.findPublishedSince(any(), any())).thenReturn(List.of(events));
    }

    // --- Correspondance ---

    @Test
    void shouldMatchOnCity() {
        givenCandidates(
                event("Maraude Lyon", "Lyon", null, null, EventType.MARAUDE, OrganizationDomain.SOCIAL),
                event("Maraude Paris", "Paris", null, null, EventType.MARAUDE, OrganizationDomain.SOCIAL));

        List<Event> matches = service.findMatches(alert("Lyon", null, null, null, null, null), now);

        assertThat(matches).extracting(Event::getTitle).containsExactly("Maraude Lyon");
    }

    @Test
    void shouldMatchWithinTheRadius() {
        // Lyon ~392 km de Paris : hors d'un rayon de 25 km autour de Paris.
        givenCandidates(
                event("Proche", "Paris", 48.8666, 2.3622, EventType.MARAUDE, OrganizationDomain.SOCIAL),
                event("Loin", "Lyon", 45.7640, 4.8357, EventType.MARAUDE, OrganizationDomain.SOCIAL));

        List<Event> matches = service.findMatches(
                alert(null, 48.8566, 2.3522, 25, null, null), now);

        assertThat(matches).extracting(Event::getTitle).containsExactly("Proche");
    }

    @Test
    void shouldExcludeMissionsWithoutCoordinatesFromARadiusSearch() {
        // Sans coordonnées, impossible d'affirmer qu'elle est dans le périmètre.
        givenCandidates(event("Sans lieu", "Paris", null, null,
                EventType.MARAUDE, OrganizationDomain.SOCIAL));

        assertThat(service.findMatches(alert(null, 48.8566, 2.3522, 25, null, null), now)).isEmpty();
    }

    @Test
    void shouldFilterOnDomainsAndTypes() {
        givenCandidates(
                event("Bonne", "Lyon", null, null, EventType.MARAUDE, OrganizationDomain.SOCIAL),
                event("Mauvais type", "Lyon", null, null, EventType.FORMATION, OrganizationDomain.SOCIAL),
                event("Mauvais domaine", "Lyon", null, null, EventType.MARAUDE, OrganizationDomain.SPORT));

        List<Event> matches = service.findMatches(
                alert(null, null, null, null, "SOCIAL", "MARAUDE"), now);

        assertThat(matches).extracting(Event::getTitle).containsExactly("Bonne");
    }

    @Test
    void emptyCriteriaShouldMatchEverything() {
        givenCandidates(
                event("A", "Lyon", null, null, EventType.MARAUDE, OrganizationDomain.SOCIAL),
                event("B", "Paris", null, null, EventType.FORMATION, OrganizationDomain.SPORT));

        assertThat(service.findMatches(alert(null, null, null, null, null, null), now)).hasSize(2);
    }

    @Test
    void shouldCapTheNumberOfMissionsSent() {
        Event[] many = new Event[10];
        for (int i = 0; i < many.length; i++) {
            many[i] = event("Mission " + i, "Lyon", null, null,
                    EventType.MARAUDE, OrganizationDomain.SOCIAL);
        }
        givenCandidates(many);

        assertThat(service.findMatches(alert(null, null, null, null, null, null), now))
                .hasSize(MissionAlertService.MAX_MATCHES_PER_ALERT);
    }

    // --- Cadence ---

    @Test
    void weeklyAlertIsDueOnlyAfterSevenDays() {
        MissionAlert a = alert(null, null, null, null, null, null);

        a.setLastSentAt(now.minusDays(3));
        assertThat(MissionAlertJob.isDue(a, now)).isFalse();

        a.setLastSentAt(now.minusDays(7));
        assertThat(MissionAlertJob.isDue(a, now)).isTrue();
    }

    @Test
    void dailyAlertIsDueAfterOneDay() {
        MissionAlert a = alert(null, null, null, null, null, null);
        a.setFrequency(AlertFrequency.DAILY);

        a.setLastSentAt(now.minusHours(6));
        assertThat(MissionAlertJob.isDue(a, now)).isFalse();

        a.setLastSentAt(now.minusDays(1));
        assertThat(MissionAlertJob.isDue(a, now)).isTrue();
    }

    // --- Création ---

    @Test
    void shouldRefuseBeyondTheAlertQuota() {
        when(alertRepository.countByUserId(user.getId()))
                .thenReturn((long) MissionAlertService.MAX_ALERTS_PER_USER);

        assertThatThrownBy(() -> service.create(user.getId(),
                new MissionAlertRequest("Trop", null, null, null, null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("alertes");
    }

    @Test
    void shouldRefuseCoordinatesWithoutRadius() {
        assertThatThrownBy(() -> service.create(user.getId(),
                new MissionAlertRequest("Bancale", null, BigDecimal.valueOf(48.85),
                        BigDecimal.valueOf(2.35), null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("rayon");
    }

    @Test
    void shouldRefuseAnOutOfRangeRadius() {
        assertThatThrownBy(() -> service.create(user.getId(),
                new MissionAlertRequest("Trop large", null, BigDecimal.valueOf(48.85),
                        BigDecimal.valueOf(2.35), 900, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("rayon");
    }

    @Test
    void newAlertShouldNotReplayHistory() {
        // lastSentAt positionné à la création : sinon le premier envoi remonterait
        // toutes les missions publiées depuis toujours.
        var created = service.create(user.getId(),
                new MissionAlertRequest("Neuve", "Lyon", null, null, null,
                        List.of("SOCIAL"), List.of("MARAUDE"), AlertFrequency.WEEKLY, true));

        assertThat(created.lastSentAt()).isNotNull();
        assertThat(created.domains()).containsExactly("SOCIAL");
        assertThat(created.types()).containsExactly("MARAUDE");
    }

    @Test
    void shouldRefuseTouchingSomeoneElsesAlert() {
        MissionAlert other = alert(null, null, null, null, null, null);
        User someoneElse = new User();
        someoneElse.setId(UUID.randomUUID());
        other.setUser(someoneElse);
        other.setId(UUID.randomUUID());
        when(alertRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.delete(user.getId(), other.getId()))
                .isInstanceOf(ForbiddenException.class);
    }
}
