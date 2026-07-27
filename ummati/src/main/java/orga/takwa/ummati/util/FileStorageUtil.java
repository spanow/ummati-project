package orga.takwa.ummati.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class FileStorageUtil {

    private final Path uploadDir;

    public FileStorageUtil(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le répertoire d'upload: " + uploadDir, e);
        }
    }

    /**
     * Stocke un fichier et retourne le chemin relatif.
     */
    public String store(MultipartFile file, String subDir) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + extension;

        Path targetDir = uploadDir.resolve(subDir);
        Files.createDirectories(targetDir);
        Path targetPath = targetDir.resolve(filename);
        file.transferTo(targetPath);

        return subDir + "/" + filename;
    }

    /**
     * Stocke un fichier en imposant l'extension (déduite du contenu réel, pas du nom
     * fourni par le client) et retourne le chemin relatif.
     */
    public String store(MultipartFile file, String subDir, String forcedExtension) throws IOException {
        String filename = UUID.randomUUID() + forcedExtension;

        Path targetDir = uploadDir.resolve(subDir);
        Files.createDirectories(targetDir);
        Path targetPath = targetDir.resolve(filename);
        file.transferTo(targetPath);

        return subDir + "/" + filename;
    }

    public void delete(String relativePath) throws IOException {
        Path path = uploadDir.resolve(relativePath);
        Files.deleteIfExists(path);
    }

    public Path resolve(String relativePath) {
        return uploadDir.resolve(relativePath);
    }
}

