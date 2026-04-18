package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.document.DocumentResponse;
import orga.takwa.ummati.entity.Document;
import orga.takwa.ummati.entity.enums.DocumentOwnerType;
import orga.takwa.ummati.entity.enums.MembershipRole;
import orga.takwa.ummati.entity.enums.MembershipStatus;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.DocumentRepository;
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

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository,
                           MembershipRepository membershipRepository, OrganizationService organizationService) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.organizationService = organizationService;
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
        boolean isMember = membershipRepository.findByUserIdAndOrganizationId(userId, orgId)
                .map(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .orElse(false);
        if (!isMember) throw new ForbiddenException("Vous devez être membre de cette organisation");

        return documentRepository.findByOwnerTypeAndOwnerId(DocumentOwnerType.ORGANIZATION, orgId)
                .stream().map(this::toResponse).toList();
    }

    // T-110: Download document
    @Transactional(readOnly = true)
    public Resource download(UUID userId, UUID docId) throws IOException {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé"));

        // Verify access: must be member of the org
        if (doc.getOwnerType() == DocumentOwnerType.ORGANIZATION) {
            boolean isMember = membershipRepository.findByUserIdAndOrganizationId(userId, doc.getOwnerId())
                    .map(m -> m.getStatus() == MembershipStatus.ACTIVE)
                    .orElse(false);
            if (!isMember) throw new ForbiddenException("Accès non autorisé");
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
        }

        Path path = Paths.get(doc.getFilePath());
        Files.deleteIfExists(path);
        documentRepository.delete(doc);
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

