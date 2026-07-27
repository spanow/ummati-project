package orga.takwa.ummati.service;

import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.util.FileStorageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock private FileStorageUtil fileStorageUtil;

    @InjectMocks private ImageService imageService;

    private static final byte[] JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    private static final byte[] PNG_HEADER = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @Test
    void store_shouldAcceptJpeg_andReturnPublicUrl() throws IOException {
        when(fileStorageUtil.store(any(), anyString(), anyString()))
                .thenReturn("images/events/42/abc.jpg");

        String url = imageService.store(file(JPEG_HEADER), "events/42");

        assertThat(url).isEqualTo("/uploads/images/events/42/abc.jpg");
        verify(fileStorageUtil).store(any(), eq("images/events/42"), eq(".jpg"));
    }

    @Test
    void store_shouldAcceptPng() throws IOException {
        when(fileStorageUtil.store(any(), anyString(), anyString()))
                .thenReturn("images/organizations/1/logo.png");

        imageService.store(file(PNG_HEADER), "organizations/1");

        verify(fileStorageUtil).store(any(), anyString(), eq(".png"));
    }

    @Test
    void store_shouldAcceptWebp() throws IOException {
        byte[] webp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        when(fileStorageUtil.store(any(), anyString(), anyString()))
                .thenReturn("images/users/1/a.webp");

        imageService.store(file(webp), "users/1");

        verify(fileStorageUtil).store(any(), anyString(), eq(".webp"));
    }

    @Test
    void store_shouldRejectFileWhoseBytesAreNotAnImage_evenWithImageContentType() {
        // Un SVG annoncé comme du JPEG : servi tel quel par le handler statique, il
        // exécuterait son script dans le navigateur de la victime.
        MockMultipartFile svg = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes());

        assertThatThrownBy(() -> imageService.store(svg, "events/1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("JPG, PNG et WebP");
    }

    @Test
    void store_shouldForceExtensionFromContent_notFromFilename() throws IOException {
        when(fileStorageUtil.store(any(), anyString(), anyString())).thenReturn("images/x/y.jpg");
        MockMultipartFile disguised = new MockMultipartFile(
                "file", "payload.html", "image/jpeg", JPEG_HEADER);

        imageService.store(disguised, "x");

        ArgumentCaptor<String> extension = ArgumentCaptor.forClass(String.class);
        verify(fileStorageUtil).store(any(), anyString(), extension.capture());
        assertThat(extension.getValue()).isEqualTo(".jpg");
    }

    @Test
    void store_shouldRejectOversizedFile() {
        byte[] tooBig = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(JPEG_HEADER, 0, tooBig, 0, JPEG_HEADER.length);

        assertThatThrownBy(() -> imageService.store(file(tooBig), "events/1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("5 Mo");
    }

    @Test
    void store_shouldRejectEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> imageService.store(empty, "events/1"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deleteByPublicUrl_shouldStripPrefix() throws IOException {
        imageService.deleteByPublicUrl("/uploads/images/events/42/abc.jpg");

        verify(fileStorageUtil).delete("images/events/42/abc.jpg");
    }

    @Test
    void deleteByPublicUrl_shouldIgnoreNullAndForeignUrls() throws IOException {
        imageService.deleteByPublicUrl(null);
        imageService.deleteByPublicUrl("https://cdn.example.com/logo.png");

        verify(fileStorageUtil, never()).delete(anyString());
    }

    @Test
    void deleteByPublicUrl_shouldRefuseTraversalPaths() throws IOException {
        imageService.deleteByPublicUrl("/uploads/../../../etc/passwd");

        verify(fileStorageUtil, never()).delete(anyString());
    }

    @Test
    void deleteByPublicUrl_shouldNotPropagateIoErrors() throws IOException {
        doThrow(new IOException("disque plein")).when(fileStorageUtil).delete(anyString());

        imageService.deleteByPublicUrl("/uploads/images/a/b.jpg");
    }

    @Test
    void replace_shouldStoreNewImageThenDeleteThePrevious() throws IOException {
        when(fileStorageUtil.store(any(), anyString(), anyString())).thenReturn("images/o/1/new.jpg");

        String url = imageService.replace(file(JPEG_HEADER), "o/1", "/uploads/images/o/1/old.jpg");

        assertThat(url).isEqualTo("/uploads/images/o/1/new.jpg");
        verify(fileStorageUtil).delete("images/o/1/old.jpg");
    }

    @Test
    void replace_shouldKeepPreviousImage_whenNewOneIsRejected() throws IOException {
        MockMultipartFile bad = new MockMultipartFile("file", "a.jpg", "image/jpeg", "nope".getBytes());

        assertThatThrownBy(() -> imageService.replace(bad, "o/1", "/uploads/images/o/1/old.jpg"))
                .isInstanceOf(BusinessRuleException.class);

        verify(fileStorageUtil, never()).delete(anyString());
    }

    private MockMultipartFile file(byte[] content) {
        return new MockMultipartFile("file", "image.jpg", "image/jpeg", content);
    }
}
