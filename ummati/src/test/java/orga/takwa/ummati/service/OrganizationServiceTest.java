package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.organization.CreateOrganizationRequest;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.OrganizationStatus;
import orga.takwa.ummati.entity.enums.UserRole;
import orga.takwa.ummati.exception.ConflictException;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private EventRepository eventRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private User verifiedUser;
    private User unverifiedUser;

    @BeforeEach
    void setUp() {
        verifiedUser = new User();
        verifiedUser.setId(UUID.randomUUID());
        verifiedUser.setEmail("test@test.com");
        verifiedUser.setFirstName("Test");
        verifiedUser.setLastName("User");
        verifiedUser.setEmailVerified(true);
        verifiedUser.setRole(UserRole.VOLUNTEER);

        unverifiedUser = new User();
        unverifiedUser.setId(UUID.randomUUID());
        unverifiedUser.setEmailVerified(false);
    }

    // RM-10: Only email_verified users can create an org
    @Test
    void create_shouldReject_whenEmailNotVerified() {
        when(userRepository.findById(unverifiedUser.getId())).thenReturn(Optional.of(unverifiedUser));

        var request = new CreateOrganizationRequest("Test ONG", "Description", null,
                "EDUCATION", null, "Paris", "75001", "org@test.com", null, null);

        assertThatThrownBy(() -> organizationService.create(unverifiedUser.getId(), request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Email non vérifié");
    }

    // Duplicate name → 409
    @Test
    void create_shouldReject_whenNameAlreadyExists() {
        when(userRepository.findById(verifiedUser.getId())).thenReturn(Optional.of(verifiedUser));
        when(organizationRepository.existsByName("Test ONG")).thenReturn(true);

        var request = new CreateOrganizationRequest("Test ONG", "Description", null,
                "EDUCATION", null, "Paris", "75001", "org@test.com", null, null);

        assertThatThrownBy(() -> organizationService.create(verifiedUser.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    // RM-12: Slug auto-generated
    @Test
    void create_shouldGenerateSlug() {
        when(userRepository.findById(verifiedUser.getId())).thenReturn(Optional.of(verifiedUser));
        when(organizationRepository.existsByName(any())).thenReturn(false);
        when(organizationRepository.existsBySlug(any())).thenReturn(false);
        when(organizationRepository.save(any())).thenAnswer(inv -> {
            Organization o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepository.findByOrganizationId(any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        var request = new CreateOrganizationRequest("Les Restos du Cœur", "Aide alimentaire", null,
                "SOCIAL", null, "Paris", "75001", "org@test.com", null, null);

        var result = organizationService.create(verifiedUser.getId(), request);

        assert result.slug().equals("les-restos-du-coeur") || result.slug().startsWith("les-restos-du-c");
        assert result.status().equals("PENDING");
    }
}

