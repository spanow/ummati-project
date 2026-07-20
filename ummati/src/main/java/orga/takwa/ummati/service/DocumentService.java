package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.document.DocumentResponse;
import orga.takwa.ummati.entity.Document;
import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.enums.DocumentOwnerType;
import orga.takwa.ummati.entity.enums.MembershipRole;
import orga.takwa.ummati.entity.enums.MembershipStatus;
import orga.takwa.ummati.entity.enums.UserRole;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.DocumentRepository;
import orga.takwa.ummati.repository.EventRepository;
import orga.takwa.ummati.repository.MembershipRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final OrganizationService organizationService;
    private final EventRepository eventRepository;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository,
                           MembershipRepository membershipRepository, OrganizationService organizationService,
                           EventRepository eventRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.organizationService = organizationService;
        this.eventRepository = eventRepository;
    }

    // T-110: Upload document for an organization
    @Transactional
    public DocumentResponse upload(UUID userId, UUID orgId, String name, MultipartFile file) throws IOException {
        organizationService.verifyAdmin(userId, orgId);

        validateFile(file);

        // Save to disk
        Path dir = Paths.get(uploadDir, "organizations", orgId.toString());
        Files.createDirectories(dir);
        String storedFilename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path dest = dir.resolve(storedFilename);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        Document doc = new Document();
        doc.setOwnerType(DocumentOwnerType.ORGANIZATION);
        doc.setOwnerId(orgId);
        doc.setName(name != null ? name : file.getOriginalFilename());
        doc.setOriginalName(file.getOriginalFilename());
        doc.setFilePath(dest.toString());
        doc.setContentType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setUploadedBy(userRepository.getReferenceById(userId));
        doc = documentRepository.save(doc);

        return toResponse(doc);
    }

    // T-110: List documents for an organization (members only)
    @Transactional(readOnly = true)
    public List<DocumentResponse> list(UUID userId, UUID orgId) {
        requireActiveMembership(userId, orgId, "Vous devez être membre de cette organisation");
        return documentRepository.findByOwnerTypeAndOwnerId(DocumentOwnerType.ORGANIZATION, orgId)
                .stream().map(this::toResponse).toList();
    }

    // Documents attachés à un événement : upload réservé à un admin de l'orga organisatrice.
    @Transactional
    public DocumentResponse uploadForEvent(UUID userId, UUID eventId, String name, MultipartFile file) throws IOException {
        UUID orgId = resolveEventOrgId(eventId);
        organizationService.verifyAdmin(userId, orgId);

        validateFile(file);

        Path dir = Paths.get(uploadDir, "events", eventId.toString());
        Files.createDirectories(dir);
        String storedFilename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path dest = dir.resolve(storedFilename);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        Document doc = new Document();
        doc.setOwnerType(DocumentOwnerType.EVENT);
        doc.setOwnerId(eventId);
        doc.setName(name != null ? name : file.getOriginalFilename());
        doc.setOriginalName(file.getOriginalFilename());
        doc.setFilePath(dest.toString());
        doc.setContentType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setUploadedBy(userRepository.getReferenceById(userId));
        doc = documentRepository.save(doc);

        return toResponse(doc);
    }

    // Liste des documents d'un événement : membre de l'orga organisatrice OU platform-admin (pour validation).
    @Transactional(readOnly = true)
    public List<DocumentResponse> listForEvent(UUID userId, UUID eventId) {
        UUID orgId = resolveEventOrgId(eventId);
        requireOrgMemberOrPlatformAdmin(userId, orgId, "Accès non autorisé aux documents de cet événement");
        return documentRepository.findByOwnerTypeAndOwnerId(DocumentOwnerType.EVENT, eventId)
                .stream().map(this::toResponse).toList();
    }

    // T-110: Download document
    @Transactional(readOnly = true)
    public Resource download(UUID userId, UUID docId) throws IOException {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé"));

        if (doc.getOwnerType() == DocumentOwnerType.ORGANIZATION) {
            requireActiveMembership(userId, doc.getOwnerId(), "Accès non autorisé");
        } else if (doc.getOwnerType() == DocumentOwnerType.EVENT) {
            requireOrgMemberOrPlatformAdmin(userId, resolveEventOrgId(doc.getOwnerId()), "Accès non autorisé");
        }

        Path path = Paths.get(doc.getFilePath());
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists()) throw new ResourceNotFoundException("Fichier introuvable");
        return resource;
    }

    // T-110: Delete document (admin only)
    @Transactional
    public void delete(UUID userId, UUID docId) throws IOException {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé"));

        if (doc.getOwnerType() == DocumentOwnerType.ORGANIZATION) {
            organizationService.verifyAdmin(userId, doc.getOwnerId());
        } else if (doc.getOwnerType() == DocumentOwnerType.EVENT) {
            organizationService.verifyAdmin(userId, resolveEventOrgId(doc.getOwnerId()));
        }

        Path path = Paths.get(doc.getFilePath());
        Files.deleteIfExists(path);
        documentRepository.delete(doc);
    }

    private UUID resolveEventOrgId(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé"));
        return event.getOrganization().getId();
    }

    private void requireActiveMembership(UUID userId, UUID orgId, String errorMessage) {
        boolean isMember = membershipRepository.findByUserIdAndOrganizationId(userId, orgId)
                .map(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .orElse(false);
        if (!isMember) throw new ForbiddenException(errorMessage);
    }

    private void requireOrgMemberOrPlatformAdmin(UUID userId, UUID orgId, String errorMessage) {
        boolean isMember = membershipRepository.findByUserIdAndOrganizationId(userId, orgId)
                .map(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .orElse(false);
        if (isMember || isPlatformAdmin(userId)) return;
        throw new ForbiddenException(errorMessage);
    }

    private boolean isPlatformAdmin(UUID userId) {
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(u -> u.getRole() == UserRole.PLATFORM_ADMIN)
                .orElse(false);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new BusinessRuleException("Le fichier est vide");
        if (file.getSize() > MAX_SIZE_BYTES) throw new BusinessRuleException("Le fichier dépasse la taille maximale de 10 Mo");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessRuleException("Type de fichier non autorisé. Formats acceptés : PDF, JPG, PNG, DOCX");
        }
    }

    private DocumentResponse toResponse(Document doc) {
        return new DocumentResponse(doc.getId(), doc.getName(), doc.getOriginalName(),
                doc.getContentType(), doc.getFileSize(), doc.getOwnerType().name(), doc.getOwnerId(),
                doc.getUploadedBy().getFirstName(), doc.getUploadedBy().getLastName(), doc.getCreatedAt());
    }
}

