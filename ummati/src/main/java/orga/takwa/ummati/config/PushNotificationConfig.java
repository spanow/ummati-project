package orga.takwa.ummati.config;

import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.jwt.nimbus.NimbusJwtFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;
import java.security.Security;

@Configuration
public class PushNotificationConfig {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationConfig.class);

    @Value("${app.vapid.public-key:}")
    private String vapidPublicKey;

    @Value("${app.vapid.private-key:}")
    private String vapidPrivateKey;

    @Value("${app.vapid.subject:}")
    private String vapidSubject;

    /**
     * Client Web Push, ou {@code null} si les clés VAPID ne sont pas configurées.
     *
     * <p>Les notifications push sont une commodité, pas une fonction vitale : une
     * plateforme déployée sans clés VAPID doit démarrer normalement et se contenter des
     * notifications in-app. Auparavant, la construction du client sur des clés vides
     * levait un {@code IndexOutOfBounds} au décodage et empêchait tout le démarrage —
     * une option facultative rendait l'application entière indisponible.
     */
    @Bean
    public PushService pushService() throws GeneralSecurityException {
        if (vapidPublicKey.isBlank() || vapidPrivateKey.isBlank()) {
            log.warn("Clés VAPID absentes : notifications push désactivées. "
                    + "Les notifications in-app et les emails restent actifs.");
            return null;
        }
        Security.addProvider(new BouncyCastleProvider());
        return PushService.builder()
                .withVapidPublicKey(vapidPublicKey)
                .withVapidPrivateKey(vapidPrivateKey)
                .withVapidSubject(vapidSubject)
                .withJwtFactory(new NimbusJwtFactory())
                .build();
    }
}
