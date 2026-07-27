package orga.takwa.ummati.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Exposition statique des images uploadées.
 *
 * <p>Volontairement limitée à deux sous-arbres — {@code images/} (logos, bannières,
 * couvertures, galeries) et {@code users/} (avatars historiques). Le répertoire d'upload
 * contient aussi les <strong>documents privés</strong> des ONG et des événements sous
 * {@code organizations/} et {@code events/} : les exposer ici court-circuiterait les
 * contrôles d'accès de {@code DocumentController}. Ne jamais élargir ce mapping à
 * {@code /uploads/**} sans déplacer les documents ailleurs.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final Path uploadDir;

    public WebMvcConfig(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String base = uploadDir.toUri().toString();
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        CacheControl cacheControl = CacheControl.maxAge(Duration.ofDays(7)).cachePublic();

        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(base + "images/")
                .setCacheControl(cacheControl);

        registry.addResourceHandler("/uploads/users/**")
                .addResourceLocations(base + "users/")
                .setCacheControl(cacheControl);
    }
}
