package orga.takwa.ummati.controller.profile;

import io.swagger.v3.oas.annotations.tags.Tag;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.service.AttestationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile/attestation")
@Tag(name = "Attestation", description = "Attestation de bénévolat (PDF) — self-service")
public class AttestationController {

    private final AttestationService attestationService;

    public AttestationController(AttestationService attestationService) {
        this.attestationService = attestationService;
    }

    // Attestation globale (toutes organisations cumulées)
    @GetMapping(produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> global(@CurrentUser UUID userId) {
        return pdf(attestationService.generate(userId, null));
    }

    // Attestation pour une organisation précise
    @GetMapping(value = "/organizations/{orgId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> forOrganization(@CurrentUser UUID userId, @PathVariable UUID orgId) {
        return pdf(attestationService.generate(userId, orgId));
    }

    private ResponseEntity<byte[]> pdf(byte[] bytes) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attestation-benevolat.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}
