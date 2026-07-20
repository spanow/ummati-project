package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.document.DocumentResponse;
import orga.takwa.ummati.entity.Document;
import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.Membership;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.DocumentOwnerType;
import orga.takwa.ummati.entity.enums.MembershipStatus;
import orga.takwa.ummati.entity.enums.UserRole;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.DocumentRepository;
import orga.takwa.ummati.repository.EventRepository;
import orga.takwa.ummati.repository.MembershipRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock DocumentRepository documentRepository;
    @Mock UserRepository userRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock OrganizationService organizationService;
    @Mock EventRepository eventRepository;

    @InjectMocks DocumentService documentService;

    private final UUID userId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        ReflectionTestUtils.setField(documentService, "uploadDir", tempDir.toString());
    }

    private Event eventWithOrg() {
        Organization org = new Organization();
        org.setId(orgId);
        Event event = new Event();
        event.setId(eventId);
        event.setOrganization(org);
        return event;
    }

    private void activeMember() {
        Membership m = new Membership();
        m.setStatus(MembershipStatus.ACTIVE);
        when(membershipRepository.findByUserIdAndOrganizationId(userId, orgId)).thenReturn(Optional.of(m));
    }

    // Upload : réservé à un admin de l'orga organisatrice, et attache bien le doc à l'événement
    @Test
    void uploadForEvent_shouldAttachToEvent_whenUserIsOrgAdmin() throws Exception {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithOrg()));
        User uploader = new User();
        uploader.setFirstName("Amina");
        uploader.setLastName("B.");
        when(userRepository.getReferenceById(userId)).thenReturn(uploader);
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var file = new MockMultipartFile("file", "autorisation.pdf", "application/pdf", "data".getBytes());
        DocumentResponse res = documentService.uploadForEvent(userId, eventId, null, file);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        org.mockito.Mockito.verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getOwnerType()).isEqualTo(DocumentOwnerType.EVENT);
        assertThat(captor.getValue().getOwnerId()).isEqualTo(eventId);
        assertThat(res.name()).isEqualTo("autorisation.pdf");
    }

    @Test
    void uploadForEvent_shouldReject_whenNotOrgAdmin() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithOrg()));
        doThrow(new ForbiddenException("Non admin")).when(organizationService).verifyAdmin(userId, orgId);

        var file = new MockMultipartFile("file", "x.pdf", "application/pdf", "data".getBytes());
        assertThatThrownBy(() -> documentService.uploadForEvent(userId, eventId, null, file))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void uploadForEvent_shouldReject_whenEventNotFound() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());
        var file = new MockMultipartFile("file", "x.pdf", "application/pdf", "data".getBytes());
        assertThatThrownBy(() -> documentService.uploadForEvent(userId, eventId, null, file))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // List : accessible à un membre actif de l'orga organisatrice
    @Test
    void listForEvent_shouldReturnDocs_whenUserIsOrgMember() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithOrg()));
        activeMember();
        when(documentRepository.findByOwnerTypeAndOwnerId(DocumentOwnerType.EVENT, eventId))
                .thenReturn(List.of());

        assertThat(documentService.listForEvent(userId, eventId)).isEmpty();
    }

    // List : accessible à un platform-admin (pour validation), même non membre
    @Test
    void listForEvent_shouldReturnDocs_whenUserIsPlatformAdmin() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithOrg()));
        when(membershipRepository.findByUserIdAndOrganizationId(userId, orgId)).thenReturn(Optional.empty());
        User admin = new User();
        admin.setRole(UserRole.PLATFORM_ADMIN);
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(documentRepository.findByOwnerTypeAndOwnerId(DocumentOwnerType.EVENT, eventId))
                .thenReturn(List.of());

        assertThat(documentService.listForEvent(userId, eventId)).isEmpty();
    }

    // List : refusée à un utilisateur ni membre ni platform-admin
    @Test
    void listForEvent_shouldReject_whenNotMemberNorAdmin() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithOrg()));
        when(membershipRepository.findByUserIdAndOrganizationId(userId, orgId)).thenReturn(Optional.empty());
        User volunteer = new User();
        volunteer.setRole(UserRole.VOLUNTEER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(volunteer));

        assertThatThrownBy(() -> documentService.listForEvent(userId, eventId))
                .isInstanceOf(ForbiddenException.class);
    }
}
