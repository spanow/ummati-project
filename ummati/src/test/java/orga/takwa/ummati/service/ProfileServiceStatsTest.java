package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.profile.ProfileResponse;
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

import java.math.BigDecimal;
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
    void getProfile_shouldExposeStoredValidatedHours() {
        when(eventSignupRepository.countByUserIdAndStatus(userId, SignupStatus.ATTENDED)).thenReturn(2L);
        // Heures certifiées et stockées (jamais recalculées depuis les dates).
        when(eventSignupRepository.sumValidatedHoursByUserId(userId)).thenReturn(new BigDecimal("3.50"));

        ProfileResponse profile = profileService.getProfile(userId);

        assertThat(profile.stats().volunteerHours()).isEqualTo(3.5);
        assertThat(profile.stats().eventsAttended()).isEqualTo(2L);
        assertThat(profile.stats().organizationCount()).isEqualTo(2L);
    }

    @Test
    void getProfile_shouldReturnZeroHours_whenNoValidatedHours() {
        when(eventSignupRepository.countByUserIdAndStatus(userId, SignupStatus.ATTENDED)).thenReturn(0L);
        when(eventSignupRepository.sumValidatedHoursByUserId(userId)).thenReturn(null);

        ProfileResponse profile = profileService.getProfile(userId);

        assertThat(profile.stats().volunteerHours()).isZero();
    }
}
