package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.profile.ProfileResponse;
import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.EventSignup;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.MembershipStatus;
import orga.takwa.ummati.entity.enums.SignupStatus;
import orga.takwa.ummati.repository.EventSignupRepository;
import orga.takwa.ummati.repository.MembershipRepository;
import orga.takwa.ummati.repository.SkillRepository;
import orga.takwa.ummati.repository.UserRepository;
import orga.takwa.ummati.util.FileStorageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceStatsTest {

    @Mock private UserRepository userRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private EventSignupRepository eventSignupRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private FileStorageUtil fileStorageUtil;

    @InjectMocks
    private ProfileService profileService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setEmail("jean@test.com");
        user.setFirstName("Jean");
        user.setLastName("Dupont");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(membershipRepository.countByUserIdAndStatus(userId, MembershipStatus.ACTIVE)).thenReturn(2L);
    }

    @Test
    void getProfile_shouldSumAttendedEventDurations_asVolunteerHours() {
        when(eventSignupRepository.countByUserIdAndStatus(userId, SignupStatus.ATTENDED)).thenReturn(2L);
        // 2h + 1h30 = 3.5 heures
        when(eventSignupRepository.findAttendedWithEventByUserId(userId)).thenReturn(List.of(
                signupWithDuration(120), signupWithDuration(90)));

        ProfileResponse profile = profileService.getProfile(userId);

        assertThat(profile.stats().volunteerHours()).isEqualTo(3.5);
        assertThat(profile.stats().eventsAttended()).isEqualTo(2L);
        assertThat(profile.stats().organizationCount()).isEqualTo(2L);
    }

    @Test
    void getProfile_shouldReturnZeroHours_whenNoAttendedEvents() {
        when(eventSignupRepository.countByUserIdAndStatus(userId, SignupStatus.ATTENDED)).thenReturn(0L);
        when(eventSignupRepository.findAttendedWithEventByUserId(userId)).thenReturn(List.of());

        ProfileResponse profile = profileService.getProfile(userId);

        assertThat(profile.stats().volunteerHours()).isZero();
    }

    @Test
    void getProfile_shouldIgnoreEventsWithMissingDates() {
        when(eventSignupRepository.countByUserIdAndStatus(userId, SignupStatus.ATTENDED)).thenReturn(2L);
        EventSignup noDates = new EventSignup();
        noDates.setEvent(new Event());
        when(eventSignupRepository.findAttendedWithEventByUserId(userId)).thenReturn(List.of(
                signupWithDuration(60), noDates));

        ProfileResponse profile = profileService.getProfile(userId);

        assertThat(profile.stats().volunteerHours()).isEqualTo(1.0);
    }

    private EventSignup signupWithDuration(int minutes) {
        Event event = new Event();
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 9, 0);
        event.setStartDate(start);
        event.setEndDate(start.plusMinutes(minutes));
        EventSignup signup = new EventSignup();
        signup.setEvent(event);
        signup.setStatus(SignupStatus.ATTENDED);
        return signup;
    }
}
