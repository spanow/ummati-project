package orga.takwa.ummati.config;

import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.jwt.nimbus.NimbusJwtFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;
import java.security.Security;

@Configuration
public class PushNotificationConfig {

    @Value("${app.vapid.public-key}")
    private String vapidPublicKey;

    @Value("${app.vapid.private-key}")
    private String vapidPrivateKey;

    @Value("${app.vapid.subject}")
    private String vapidSubject;

    @Bean
    public PushService pushService() throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());
        return PushService.builder()
                .withVapidPublicKey(vapidPublicKey)
                .withVapidPrivateKey(vapidPrivateKey)
                .withVapidSubject(vapidSubject)
                .withJwtFactory(new NimbusJwtFactory())
                .build();
    }
}
