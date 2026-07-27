package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.membership.MembershipActionRequest;
import orga.takwa.ummati.dto.membership.MembershipRequest;
import orga.takwa.ummati.entity.Membership;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.*;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ConflictException;
import orga.takwa.ummati.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private OrganizationService organizationService;
    @Mock private MembershipQuestionService membershipQuestionService;

    @InjectMocks
    private MembershipService membershipService;

    private User user;
    private Organization activeOrg;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Test");
        user.setLastName("User");

        orgId = UUID.randomUUID();
        activeOrg = new Organization();
        activeOrg.setId(orgId);
        activeOrg.setName("Test ONG");
        activeOrg.setSlug("test-ong");
        activeOrg.setStatus(OrganizationStatus.ACTIVE);
    }

    // RM-20: Cannot join an inactive org
    @Test
    void requestMembership_shouldReject_whenOrgNotActive() {
        activeOrg.setStatus(OrganizationStatus.PENDING);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(activeOrg));

        assertThatThrownBy(() -> membershipService.requestMembership(user.getId(), orgId, new MembershipRequest(null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("n'est pas active");
    }

    // RM-20: Already active member → 409
    @Test
    void requestMembership_shouldReject_whenAlreadyMember() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(activeOrg));
        Membership existing = new Membership();
        existing.setStatus(MembershipStatus.ACTIVE);
        when(membershipRepository.findByUserIdAndOrganizationId(user.getId(), orgId))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> membershipService.requestMembership(user.getId(), orgId, new MembershipRequest(null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("déjà membre");
    }

    // RM-21: 30-day cooldown after rejection
    @Test
    void requestMembership_shouldReject_whenRejectedRecently() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(activeOrg));
        Membership rejected = new Membership();
        rejected.setStatus(MembershipStatus.REJECTED);
        rejected.setRejectedAt(LocalDateTime.now().minusDays(10)); // 10 days ago < 30 days
        when(membershipRepository.findByUserIdAndOrganizationId(user.getId(), orgId))
                .thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> membershipService.requestMembership(user.getId(), orgId, new MembershipRequest(null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pourrez refaire");
    }

    // RM-22: Last admin cannot leave
    @Test
    void removeMembership_shouldReject_whenLastAdmin() {
        UUID membershipId = UUID.randomUUID();
        Membership m = new Membership();
        m.setId(membershipId);
        m.setUser(user);
        m.setOrganization(activeOrg);
        m.setRole(MembershipRole.ADMIN);
        m.setStatus(MembershipStatus.ACTIVE);

        when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(m));
        when(membershipRepository.countByOrganizationIdAndRoleAndStatus(orgId, MembershipRole.ADMIN, MembershipStatus.ACTIVE))
                .thenReturn(1L);

        assertThatThrownBy(() -> membershipService.removeMembership(user.getId(), membershipId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("dernier admin");
    }

    // --- Helpers ---

    private Membership buildMembership(UUID membershipId, User owner, Organization org,
                                       MembershipRole role, MembershipStatus status) {
        Membership m = new Membership();
        m.setId(membershipId);
        m.setUser(owner);
        m.setOrganization(org);
        m.setRole(role);
        m.setStatus(status);
        return m;
    }

    // --- getMyMembership ---

    // Utilisateur membre → Optional avec le bon statut et rôle
    @Test
    void getMyMembership_shouldReturnResponse_whenUserIsMember() {
        Membership m = buildMembership(UUID.randomUUID(), user, activeOrg, MembershipRole.ADMIN, MembershipStatus.ACTIVE);
        when(membershipRepository.findByUserIdAndOrganizationId(user.getId(), orgId))
                .thenReturn(Optional.of(m));

        var result = membershipService.getMyMembership(user.getId(), orgId);

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo("ACTIVE");
        assertThat(result.get().role()).isEqualTo("ADMIN");
        assertThat(result.get().userId()).isEqualTo(user.getId());
    }

    // Utilisateur non membre → Optional vide
    @Test
    void getMyMembership_shouldReturnEmpty_whenUserIsNotMember() {
        when(membershipRepository.findByUserIdAndOrganizationId(user.getId(), orgId))
                .thenReturn(Optional.empty());

        var result = membershipService.getMyMembership(user.getId(), orgId);

        assertThat(result).isEmpty();
    }

    // Utilisateur avec demande en attente → Optional avec statut PENDING
    @Test
    void getMyMembership_shouldReturnPending_whenRequestIsPending() {
        Membership m = buildMembership(UUID.randomUUID(), user, activeOrg, MembershipRole.MEMBER, MembershipStatus.PENDING);
        when(membershipRepository.findByUserIdAndOrganizationId(user.getId(), orgId))
                .thenReturn(Optional.of(m));

        var result = membershipService.getMyMembership(user.getId(), orgId);

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo("PENDING");
    }

    // RM-23: Cannot exclude another admin
    @Test
    void removeMembership_shouldReject_whenExcludingAdmin() {
        UUID adminUserId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();

        User targetUser = new User();
        targetUser.setId(UUID.randomUUID()); // different user

        Membership m = new Membership();
        m.setId(membershipId);
        m.setUser(targetUser);
        m.setOrganization(activeOrg);
        m.setRole(MembershipRole.ADMIN);
        m.setStatus(MembershipStatus.ACTIVE);

        when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(m));
        // organizationService.verifyAdmin is mocked → no-op; the service then checks the target's role directly

        assertThatThrownBy(() -> membershipService.removeMembership(adminUserId, membershipId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Impossible d'exclure un admin");
    }

    // --- Ré-adhésion : le rôle ne doit pas survivre au départ ---

    @Test
    void requestMembership_shouldResetRoleToMember_whenAFormerAdminReapplies() {
        // Un ancien ADMIN qui avait quitté l'ONG conservait son rôle sur la ligne
        // d'adhésion : à l'approbation de sa nouvelle demande, il redevenait
        // silencieusement administrateur.
        Membership previous = new Membership();
        previous.setId(UUID.randomUUID());
        previous.setUser(user);
        previous.setOrganization(activeOrg);
        previous.setRole(MembershipRole.ADMIN);
        previous.setStatus(MembershipStatus.LEFT);
        previous.setJoinedAt(LocalDateTime.now().minusMonths(6));

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(activeOrg));
        when(membershipRepository.findByUserIdAndOrganizationId(user.getId(), orgId))
                .thenReturn(Optional.of(previous));
        when(membershipRepository.save(any(Membership.class))).thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.findByOrganizationIdAndRoleAndStatus(
                orgId, MembershipRole.ADMIN, MembershipStatus.ACTIVE)).thenReturn(Collections.emptyList());

        membershipService.requestMembership(user.getId(), orgId, new MembershipRequest("Je reviens"));

        assertThat(previous.getStatus()).isEqualTo(MembershipStatus.PENDING);
        assertThat(previous.getRole()).isEqualTo(MembershipRole.MEMBER);
        assertThat(previous.getJoinedAt()).isNull();
    }

    @Test
    void requestMembership_shouldResetRoleToMember_whenReapplyingAfterTheRejectionCooldown() {
        Membership previous = new Membership();
        previous.setId(UUID.randomUUID());
        previous.setUser(user);
        previous.setOrganization(activeOrg);
        previous.setRole(MembershipRole.ADMIN);
        previous.setStatus(MembershipStatus.REJECTED);
        previous.setRejectedAt(LocalDateTime.now().minusDays(45));

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(activeOrg));
        when(membershipRepository.findByUserIdAndOrganizationId(user.getId(), orgId))
                .thenReturn(Optional.of(previous));
        when(membershipRepository.save(any(Membership.class))).thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.findByOrganizationIdAndRoleAndStatus(
                orgId, MembershipRole.ADMIN, MembershipStatus.ACTIVE)).thenReturn(Collections.emptyList());

        membershipService.requestMembership(user.getId(), orgId, new MembershipRequest("Nouvelle demande"));

        assertThat(previous.getRole()).isEqualTo(MembershipRole.MEMBER);
        assertThat(previous.getRejectedAt()).isNull();
    }
}

