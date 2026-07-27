package orga.takwa.ummati.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assainissement du nom de fichier fourni par le client.
 *
 * <p>{@code MultipartFile.getOriginalFilename()} rend la valeur brute envoyée par le
 * navigateur : concaténée telle quelle dans un chemin, elle permettait d'écrire hors du
 * répertoire d'upload.
 */
class DocumentServiceFilenameTest {

    @Test
    void shouldStripUnixTraversal() {
        assertThat(DocumentService.safeFilename("../../../../etc/cron.d/backdoor"))
                .isEqualTo("backdoor");
    }

    @Test
    void shouldStripWindowsTraversal() {
        assertThat(DocumentService.safeFilename("..\\..\\..\\Windows\\System32\\evil.dll"))
                .isEqualTo("evil.dll");
    }

    @Test
    void shouldStripAbsolutePath() {
        assertThat(DocumentService.safeFilename("/etc/passwd")).isEqualTo("passwd");
    }

    @Test
    void shouldKeepAPlainFilename() {
        assertThat(DocumentService.safeFilename("statuts-association.pdf"))
                .isEqualTo("statuts-association.pdf");
    }

    @Test
    void shouldFallBackWhenNothingUsableRemains() {
        assertThat(DocumentService.safeFilename("..")).isEqualTo("document");
        assertThat(DocumentService.safeFilename("")).isEqualTo("document");
        assertThat(DocumentService.safeFilename(null)).isEqualTo("document");
        assertThat(DocumentService.safeFilename("   ")).isEqualTo("document");
    }

    @Test
    void shouldNeutralizeCharactersThatWouldBreakTheDownloadHeader() {
        // Un guillemet ou un saut de ligne casserait l'en-tête Content-Disposition.
        String cleaned = DocumentService.safeFilename("rapport\".pdf");
        assertThat(cleaned).doesNotContain("\"");

        assertThat(DocumentService.safeFilename("rapport\r\nX-Injected: 1.pdf"))
                .doesNotContain("\r").doesNotContain("\n");
    }

    @Test
    void shouldCapVeryLongNames() {
        String name = "a".repeat(400) + ".pdf";

        assertThat(DocumentService.safeFilename(name).length()).isLessThanOrEqualTo(150);
    }

    @Test
    void shouldNeverReturnAPathSeparator() {
        assertThat(DocumentService.safeFilename("dossier/sous-dossier/fichier.pdf"))
                .doesNotContain("/").doesNotContain("\\");
    }
}
