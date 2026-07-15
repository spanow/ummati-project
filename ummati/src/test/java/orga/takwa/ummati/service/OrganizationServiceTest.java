package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.organization.CreateOrganizationRequest;
import orga.takwa.ummati.dto.organization.OrganizationStatusRequest;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.OrganizationDomain;
import orga.takwa.ummati.entity.enums.OrganizationStatus;
import orga.takwa.ummati.entity.enums.UserRole;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ConflictException;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    // --- Helpers ---

    private Organization buildPendingOrg(UUID orgId) {
        Organization org = new Organization();
        org.setId(orgId);
        org.setName("Test ONG");
        org.setSlug("test-ong");
        org.setDescription("Une organisation de test pour les bénévoles");
        org.setStatus(OrganizationStatus.PENDING);
        org.setDomain(OrganizationDomain.EDUCATION);
        org.setCreatedBy(verifiedUser);
        return org;
    }

    private void stubToDetail(UUID orgId) {
        when(membershipRepository.countByOrganizationIdAndRoleAndStatus(any(), any(), any())).thenReturn(0L);
        when(eventRepository.findByOrganizationId(any(), any())).thenReturn(Page.empty());
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

    // --- changeStatus ---

    // T-050: Approve → status ACTIVE, notification ONG_VALIDATED envoyée
    @Test
    void changeStatus_shouldApprove_whenStatusIsActive() {
        UUID adminId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        User admin = new User();
        admin.setId(adminId);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(buildPendingOrg(orgId)));
        when(userRepository.getReferenceById(adminId)).thenReturn(admin);
        when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubToDetail(orgId);

        var result = organizationService.changeStatus(adminId, orgId, new OrganizationStatusRequest("ACTIVE", null));

        assertThat(result.status()).isEqualTo("ACTIVE");
        verify(notificationRepository).save(any());
    }

    // T-050: Reject avec motif valide → status REJECTED, motif persisté, notification envoyée
    @Test
    void changeStatus_shouldReject_whenStatusIsRejectedWithValidReason() {
        UUID adminId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        String reason = "Le dossier est incomplet : les statuts de l'association sont manquants.";

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(buildPendingOrg(orgId)));
        when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubToDetail(orgId);

        var result = organizationService.changeStatus(adminId, orgId, new OrganizationStatusRequest("REJECTED", reason));

        assertThat(result.status()).isEqualTo("REJECTED");
        assertThat(result.rejectionReason()).isEqualTo(reason);
        verify(notificationRepository).save(any());
    }

    // T-050: Reject sans motif → BusinessRuleException
    @Test
    void changeStatus_shouldThrow_whenRejectingWithNullReason() {
        UUID orgId = UUID.randomUUID();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(buildPendingOrg(orgId)));

        assertThatThrownBy(() -> organizationService.changeStatus(
                UUID.randomUUID(), orgId, new OrganizationStatusRequest("REJECTED", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("20 caractères");
    }

    // T-050: Reject motif vide (espaces) → BusinessRuleException
    @Test
    void changeStatus_shouldThrow_whenRejectingWithBlankReason() {
        UUID orgId = UUID.randomUUID();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(buildPendingOrg(orgId)));

        assertThatThrownBy(() -> organizationService.changeStatus(
                UUID.randomUUID(), orgId, new OrganizationStatusRequest("REJECTED", "   ")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("20 caractères");
    }

    // T-050: Reject motif trop court (< 20 chars) → BusinessRuleException
    @Test
    void changeStatus_shouldThrow_whenRejectingWithShortReason() {
        UUID orgId = UUID.randomUUID();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(buildPendingOrg(orgId)));

        assertThatThrownBy(() -> organizationService.changeStatus(
                UUID.randomUUID(), orgId, new OrganizationStatusRequest("REJECTED", "Trop court")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("20 caractères");
    }
}

