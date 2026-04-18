package orga.takwa.ummati.controller.document;

import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.document.DocumentResponse;
import orga.takwa.ummati.service.DocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // T-110: Upload document for organization
    @PostMapping("/organizations/{orgId}/documents")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @CurrentUser UUID userId,
            @PathVariable UUID orgId,
            @RequestParam("name") String name,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(documentService.upload(userId, orgId, name, file)));
    }

    // T-110: List documents of organization (members)
    @GetMapping("/organizations/{orgId}/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> list(
            @CurrentUser UUID userId,
            @PathVariable UUID orgId) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.list(userId, orgId)));
    }

    // T-110: Download document
    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> download(
            @CurrentUser UUID userId,
            @PathVariable UUID id) throws IOException {
        Resource resource = documentService.download(userId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // T-110: Delete document (admin)
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> delete(
            @CurrentUser UUID userId,
            @PathVariable UUID id) throws IOException {
        documentService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}

