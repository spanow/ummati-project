package orga.takwa.ummati;

import orga.takwa.ummati.config.SecretsValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class UmmatiApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(UmmatiApplication.class);
        // Vérifie les secrets dès que l'environnement est prêt, avant toute création de
        // bean : en profil prod, un secret resté à la valeur du dépôt doit arrêter le
        // démarrage net, pas se perdre derrière une erreur de connexion à la base.
        app.addListeners(new SecretsValidator());
        app.run(args);
    }

}
