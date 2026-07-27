package orga.takwa.ummati.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Refuse le démarrage en production tant qu'un secret est resté à sa valeur par défaut
 * ou est trop faible.
 *
 * <p>Les valeurs par défaut de {@code application.yaml} existent pour que le projet se
 * lance sans configuration en développement. En production, elles sont publiques — elles
 * sont dans le dépôt : quiconque les lit peut signer un jeton {@code PLATFORM_ADMIN} et
 * prendre le contrôle de la plateforme. Oublier une variable d'environnement au
 * déploiement ne doit donc pas donner une application qui démarre « normalement », mais
 * une application qui refuse de démarrer.
 *
 * <p>Branché sur {@link ApplicationEnvironmentPreparedEvent} et non déclaré en bean : la
 * vérification doit précéder la création du contexte. En bean, l'échec de connexion à la
 * base survenait avant elle et le vrai motif — un secret manquant — se perdait sous une
 * pile Hibernate. L'écouteur est enregistré explicitement dans
 * {@code UmmatiApplication.main} plutôt que par fichier SPI, dont la convention de nommage
 * a changé avec Spring Boot 4.
 *
 * <p>Actif uniquement sur le profil {@code prod} : les tests et le développement local
 * continuent d'utiliser les valeurs par défaut.
 */
public class SecretsValidator implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    /** Longueur minimale de la clé de signature JWT : HS256 exige 256 bits. */
    private static final int JWT_MIN_BYTES = 32;

    private static final int PASSWORD_MIN_LENGTH = 12;

    /**
     * Valeurs présentes dans le dépôt. Toute correspondance signifie que la variable
     * d'environnement correspondante n'a pas été fournie.
     */
    static final Set<String> REPOSITORY_DEFAULTS = Set.of(
            "changeThisSecretInProductionItMustBeAtLeast256BitsLong!!",
            "Admin123!",
            "BPLSJxSB3QAA1XsJVFED-6HGGyJmFios6ETEo9MJ-7RmbPOXioh_0WsOUPorDEpCbP_PaEJ3wGyo0t3f93y2sU4",
            "AM_NeFGDjso48CY-G965jKrTatAREp-TA-1KCPcJgbJI"
    );

    /** S'exécute en dernier pour que application.yaml et les profils soient résolus. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        validate(event.getEnvironment());
    }

    void validate(ConfigurableEnvironment environment) {
        if (!isProduction(environment)) {
            return;
        }
        List<String> problems = collectProblems(
                environment.getProperty("app.jwt.secret"),
                environment.getProperty("app.admin.password"),
                environment.getProperty("spring.datasource.password"),
                environment.getProperty("app.vapid.public-key"),
                environment.getProperty("app.vapid.private-key"));

        if (!problems.isEmpty()) {
            throw new IllegalStateException(formatMessage(problems));
        }
    }

    private boolean isProduction(ConfigurableEnvironment environment) {
        List<String> profiles = new ArrayList<>(Arrays.asList(environment.getActiveProfiles()));
        String declared = environment.getProperty("spring.profiles.active", "");
        profiles.addAll(Arrays.asList(declared.split(",")));
        return profiles.stream().map(String::trim).anyMatch("prod"::equalsIgnoreCase);
    }

    /** Extrait pour être testable sans démarrer de contexte Spring. */
    static List<String> collectProblems(String jwtSecret, String adminPassword, String datasourcePassword,
                                        String vapidPublicKey, String vapidPrivateKey) {
        List<String> problems = new ArrayList<>();

        check(problems, "JWT_SECRET", jwtSecret, true);
        check(problems, "ADMIN_PASSWORD", adminPassword, true);
        check(problems, "DB_PASSWORD", datasourcePassword, true);
        // Clés VAPID optionnelles : sans elles, seules les notifications push sont
        // indisponibles, le reste de la plateforme fonctionne.
        check(problems, "VAPID_PUBLIC_KEY", vapidPublicKey, false);
        check(problems, "VAPID_PRIVATE_KEY", vapidPrivateKey, false);

        if (isSet(jwtSecret) && jwtSecret.getBytes(StandardCharsets.UTF_8).length < JWT_MIN_BYTES) {
            problems.add("JWT_SECRET fait moins de " + JWT_MIN_BYTES
                    + " octets : HS256 exige une clé d'au moins 256 bits");
        }
        if (isSet(adminPassword) && adminPassword.length() < PASSWORD_MIN_LENGTH) {
            problems.add("ADMIN_PASSWORD fait moins de " + PASSWORD_MIN_LENGTH + " caractères");
        }
        return problems;
    }

    static String formatMessage(List<String> problems) {
        StringBuilder sb = new StringBuilder("\n\n"
                + "========================================================================\n"
                + " DÉMARRAGE REFUSÉ — secrets de production non configurés\n"
                + "========================================================================");
        problems.forEach(p -> sb.append("\n  - ").append(p));
        sb.append("\n\nGénération de chaque secret : voir DEPLOIEMENT.md\n")
          .append("========================================================================\n");
        return sb.toString();
    }

    private static void check(List<String> problems, String envVar, String value, boolean required) {
        if (!isSet(value)) {
            if (required) {
                problems.add(envVar + " n'est pas défini");
            }
            return;
        }
        if (REPOSITORY_DEFAULTS.contains(value)) {
            problems.add(envVar + " est resté à la valeur par défaut du dépôt (publique, donc compromise)");
        }
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
