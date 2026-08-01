package orga.takwa.ummati.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client de notifications natives, ou rien du tout.
 *
 * <p>Même parti pris que {@link PushNotificationConfig} pour les clés VAPID : le push
 * est une commodité et son absence de configuration ne doit jamais empêcher
 * l'application de démarrer. Un environnement sans identifiants FCM — le poste de
 * développement, la CI — fonctionne normalement avec les notifications in-app et les
 * emails.
 */
@Configuration
public class FcmConfig {

    private static final Logger log = LoggerFactory.getLogger(FcmConfig.class);

    /** Contenu JSON du compte de service, injecté par variable d'environnement. */
    @Value("${app.fcm.credentials-json:}")
    private String credentialsJson;

    /** Alternative par fichier, pratique en local. */
    @Value("${app.fcm.credentials-path:}")
    private String credentialsPath;

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        GoogleCredentials credentials = loadCredentials();
        if (credentials == null) {
            log.warn("Identifiants FCM absents : notifications natives désactivées. "
                    + "Les notifications in-app, les emails et le Web Push restent actifs.");
            return null;
        }

        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            // FirebaseApp tient un registre statique global : réinitialiser l'instance
            // par défaut lève IllegalStateException. Le contexte Spring peut être
            // reconstruit plusieurs fois dans une même JVM (tests, devtools), d'où cette
            // réutilisation plutôt qu'une initialisation systématique.
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();

            log.info("Notifications natives FCM activées");
            return FirebaseMessaging.getInstance(app);
        } catch (Exception e) {
            // Des identifiants mal formés ne doivent pas plus bloquer le démarrage que
            // des identifiants absents : on repasse simplement en mode dégradé.
            log.error("Initialisation FCM impossible, notifications natives désactivées : {}", e.getMessage());
            return null;
        }
    }

    private GoogleCredentials loadCredentials() {
        try {
            if (credentialsJson != null && !credentialsJson.isBlank()) {
                return GoogleCredentials.fromStream(
                        new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)));
            }
            if (credentialsPath != null && !credentialsPath.isBlank()) {
                Path path = Path.of(credentialsPath);
                if (!Files.isReadable(path)) {
                    log.warn("Fichier d'identifiants FCM illisible : {}", credentialsPath);
                    return null;
                }
                try (var in = Files.newInputStream(path)) {
                    return GoogleCredentials.fromStream(in);
                }
            }
            return null;
        } catch (IOException e) {
            log.error("Lecture des identifiants FCM impossible : {}", e.getMessage());
            return null;
        }
    }
}
