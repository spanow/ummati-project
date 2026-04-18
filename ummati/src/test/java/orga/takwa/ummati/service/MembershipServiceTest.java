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
    @Mock private NotificationRepository notificationRepository;
    @Mock private AuditLogRepository auditLogRepository;

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

        assertThatThrownBy(() -> membershipService.requestMembership(user.getId(), orgId, new MembershipRequest(null)))
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

        assertThatThrownBy(() -> membershipService.requestMembership(user.getId(), orgId, new MembershipRequest(null)))
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

        assertThatThrownBy(() -> membershipService.requestMembership(user.getId(), orgId, new MembershipRequest(null)))
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

        // Actor is admin
        Membership actorMembership = new Membership();
        actorMembership.setRole(MembershipRole.ADMIN);
        actorMembership.setStatus(MembershipStatus.ACTIVE);

        when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(m));
        when(membershipRepository.findByUserIdAndOrganizationId(adminUserId, orgId))
                .thenReturn(Optional.of(actorMembership));

        assertThatThrownBy(() -> membershipService.removeMembership(adminUserId, membershipId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Impossible d'exclure un admin");
    }
}

