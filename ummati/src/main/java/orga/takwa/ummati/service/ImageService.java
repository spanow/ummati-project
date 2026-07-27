package orga.takwa.ummati.service;

import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.util.FileStorageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Point d'entrée unique pour le stockage des images (avatars, logos, bannières,
 * couvertures d'événement, galeries).
 *
 * <p>Deux garanties par rapport à un upload naïf :
 * <ul>
 *   <li>le type est déterminé à partir des octets du fichier, jamais du {@code Content-Type}
 *       déclaré ni de l'extension du nom fourni — un SVG ou un HTML renommé en {@code .jpg}
 *       serait servi tel quel par le handler statique et deviendrait un vecteur XSS ;</li>
 *   <li>les images vivent sous {@code {uploadDir}/images/…} (et {@code users/…} pour
 *       l'historique des avatars), un sous-arbre distinct de celui des documents privés
 *       ({@code organizations/…}, {@code events/…}) que seul le endpoint de téléchargement
 *       contrôlé doit exposer.</li>
 * </ul>
 */
@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    /** Préfixe public des images servies statiquement. */
    public static final String PUBLIC_PREFIX = "/uploads/";

    /** Sous-arbre réservé aux images, jamais aux documents. */
    public static final String IMAGE_ROOT = "images";

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final FileStorageUtil fileStorageUtil;

    public ImageService(FileStorageUtil fileStorageUtil) {
        this.fileStorageUtil = fileStorageUtil;
    }

    /** Formats acceptés, identifiés par leur signature binaire. */
    private enum ImageFormat {
        JPEG(".jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
        PNG(".png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
        WEBP(".webp", null); // RIFF….WEBP, contrôlé séparément (signature discontinue)

        final String extension;
        final byte[] magic;

        ImageFormat(String extension, byte[] magic) {
            this.extension = extension;
            this.magic = magic;
        }
    }

    /**
     * Valide puis stocke une image.
     *
     * @param subDir sous-dossier relatif à {@link #IMAGE_ROOT} (ex. {@code "events/<id>"})
     * @return l'URL publique de l'image (ex. {@code /uploads/images/events/<id>/<uuid>.jpg})
     */
    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Aucun fichier fourni");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessRuleException("L'image ne doit pas dépasser 5 Mo");
        }

        ImageFormat format = detectFormat(file);
        try {
            String relativePath = fileStorageUtil.store(file, IMAGE_ROOT + "/" + subDir, format.extension);
            return PUBLIC_PREFIX + relativePath;
        } catch (IOException e) {
            throw new BusinessRuleException("Échec de l'enregistrement de l'image");
        }
    }

    /**
     * Supprime le fichier correspondant à une URL publique précédemment retournée par
     * {@link #store}. Silencieux : la suppression d'un fichier déjà absent (ou d'une URL
     * externe) ne doit jamais faire échouer l'opération métier appelante.
     */
    public void deleteByPublicUrl(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(PUBLIC_PREFIX)) {
            return;
        }
        String relativePath = publicUrl.substring(PUBLIC_PREFIX.length());
        // Défense en profondeur : une valeur en base ne doit jamais permettre de remonter
        // hors du répertoire d'upload.
        if (relativePath.contains("..")) {
            log.warn("Chemin d'image suspect ignoré à la suppression : {}", publicUrl);
            return;
        }
        try {
            fileStorageUtil.delete(relativePath);
        } catch (IOException e) {
            log.warn("Impossible de supprimer l'image {} : {}", publicUrl, e.getMessage());
        }
    }

    /** Remplace une image : stocke la nouvelle puis supprime l'ancienne. */
    public String replace(MultipartFile file, String subDir, String previousPublicUrl) {
        String newUrl = store(file, subDir);
        deleteByPublicUrl(previousPublicUrl);
        return newUrl;
    }

    private ImageFormat detectFormat(MultipartFile file) {
        byte[] header = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(header, 0, header.length);
        } catch (IOException e) {
            throw new BusinessRuleException("Fichier illisible");
        }

        if (read >= 3 && startsWith(header, ImageFormat.JPEG.magic)) {
            return ImageFormat.JPEG;
        }
        if (read >= 8 && startsWith(header, ImageFormat.PNG.magic)) {
            return ImageFormat.PNG;
        }
        // WEBP : "RIFF" (0-3) + taille (4-7) + "WEBP" (8-11)
        if (read >= 12
                && startsWith(header, new byte[]{'R', 'I', 'F', 'F'})
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return ImageFormat.WEBP;
        }
        throw new BusinessRuleException("Seules les images JPG, PNG et WebP sont acceptées");
    }

    private boolean startsWith(byte[] header, byte[] prefix) {
        return Arrays.equals(header, 0, prefix.length, prefix, 0, prefix.length);
    }
}
