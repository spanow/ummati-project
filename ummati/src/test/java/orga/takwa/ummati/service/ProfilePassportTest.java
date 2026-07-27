package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.profile.VolunteerPassport;
import orga.takwa.ummati.entity.*;
import orga.takwa.ummati.entity.enums.OrganizationDomain;
import orga.takwa.ummati.entity.enums.SignupStatus;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.EventSignupRepository;
import orga.takwa.ummati.repository.MembershipRepository;
import orga.takwa.ummati.repository.SkillRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfilePassportTest {

    @Mock private UserRepository userRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private EventSignupRepository eventSignupRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ImageService imageService;

    @InjectMocks private ProfileService profileService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setFirstName("Jean");
        user.setLastName("Dupont");
        user.setAddressCity("Lyon");
        user.setEnabled(true);
    }

    @Test
    void publicPassport_shouldBeHidden_whenVolunteerHasNotOptedIn() {
        user.setProfilePublic(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> profileService.getPublicPassport(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void publicPassport_shouldBeHidden_whenAccountIsDisabled() {
        user.setProfilePublic(true);
        user.setEnabled(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> profileService.getPublicPassport(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void publicPassport_shouldReduceLastNameToItsInitial() {
        user.setProfilePublic(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventSignupRepository.findAttendedWithEventByUserId(userId)).thenReturn(List.of());

        VolunteerPassport passport = profileService.getPublicPassport(userId);

        assertThat(passport.firstName()).isEqualTo("Jean");
        assertThat(passport.lastName()).isEqualTo("D.");
    }

    @Test
    void ownPassport_shouldKeepFullName_evenWhenNotPublished() {
        user.setProfilePublic(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventSignupRepository.findAttendedWithEventByUserId(userId)).thenReturn(List.of());

        VolunteerPassport passport = profileService.getOwnPassport(userId);

        assertThat(passport.lastName()).isEqualTo("Dupont");
        assertThat(passport.profilePublic()).isFalse();
    }

    @Test
    void passport_shouldAggregateMissionsOrganizationsAndCauses() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        Organization croixRouge = org("Croix-Rouge", "croix-rouge", OrganizationDomain.SANTE);
        Organization resto = org("Resto du Coeur", "resto", OrganizationDomain.SOCIAL);
        when(eventSignupRepository.findAttendedWithEventByUserId(userId)).thenReturn(List.of(
                attendedSignup(croixRouge, "Maraude", 120, null),
                attendedSignup(croixRouge, "Collecte", 60, null),
                attendedSignup(resto, "Distribution", 90, null)));

        VolunteerPassport passport = profileService.getOwnPassport(userId);

        assertThat(passport.missionsCompleted()).isEqualTo(3);
        assertThat(passport.organizationCount()).isEqualTo(2);
        // Causes triées par nombre de missions décroissant
        assertThat(passport.causes()).hasSize(2);
        assertThat(passport.causes().get(0).domain()).isEqualTo("SANTE");
        assertThat(passport.causes().get(0).missionCount()).isEqualTo(2);
        assertThat(passport.hoursTotal()).isEqualTo(4.5);
    }

    @Test
    void passport_shouldPreferHoursValidatedByTheOrganization_overEventDuration() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        Organization o = org("ONG", "ong", OrganizationDomain.SOCIAL);
        // Créneau de 3h mais l'ONG n'en a validé que 2 (le bénévole est parti plus tôt).
        when(eventSignupRepository.findAttendedWithEventByUserId(userId)).thenReturn(List.of(
                attendedSignup(o, "Mission", 180, new BigDecimal("2.0"))));

        VolunteerPassport passport = profileService.getOwnPassport(userId);

        assertThat(passport.hoursTotal()).isEqualTo(2.0);
    }

    @Test
    void passport_shouldKeepAtMostFiveRecentMissions_mostRecentFirst() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        Organization o = org("ONG", "ong", OrganizationDomain.SOCIAL);
        List<EventSignup> signups = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            EventSignup s = attendedSignup(o, "Mission " + i, 60, null);
            s.getEvent().setStartDate(LocalDateTime.of(2026, 1, 1, 9, 0).plusDays(i));
            s.getEvent().setEndDate(s.getEvent().getStartDate().plusHours(1));
            signups.add(s);
        }
        when(eventSignupRepository.findAttendedWithEventByUserId(userId)).thenReturn(signups);

        VolunteerPassport passport = profileService.getOwnPassport(userId);

        assertThat(passport.recentMissions()).hasSize(5);
        assertThat(passport.recentMissions().get(0).title()).isEqualTo("Mission 6");
        assertThat(passport.missionsCompleted()).isEqualTo(7);
    }

    @Test
    void setPassportVisibility_shouldTogglePublicationFlag() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventSignupRepository.findAttendedWithEventByUserId(userId)).thenReturn(List.of());

        VolunteerPassport passport = profileService.setPassportVisibility(userId, true);

        assertThat(passport.profilePublic()).isTrue();
        assertThat(user.isProfilePublic()).isTrue();
    }

    private Organization org(String name, String slug, OrganizationDomain domain) {
        Organization o = new Organization();
        o.setId(UUID.randomUUID());
        o.setName(name);
        o.setSlug(slug);
        o.setDomain(domain);
        return o;
    }

    private EventSignup attendedSignup(Organization org, String title, int minutes, BigDecimal validatedHours) {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle(title);
        event.setOrganization(org);
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 9, 0);
        event.setStartDate(start);
        event.setEndDate(start.plusMinutes(minutes));

        EventSignup signup = new EventSignup();
        signup.setEvent(event);
        signup.setStatus(SignupStatus.ATTENDED);
        signup.setHoursValidated(validatedHours);
        return signup;
    }
}
