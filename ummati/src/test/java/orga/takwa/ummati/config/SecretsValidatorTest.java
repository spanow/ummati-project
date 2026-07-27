package orga.takwa.ummati.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Garde-fou de mise en production : l'application doit refuser de démarrer plutôt que
 * de tourner avec la clé de signature publiée dans le dépôt.
 */
class SecretsValidatorTest {

    private static final String REPO_JWT = "changeThisSecretInProductionItMustBeAtLeast256BitsLong!!";
    private static final String STRONG_JWT = "9f2c7b1e4a6d8035c1e7a94b2f6d08e3a5c7194b2e6d8f03a1c5e79b4d2f6801";
    private static final String STRONG_ADMIN_PWD = "MotDePasseAdmin2026!";

    // --- Règles de validation ---

    @Test
    void shouldReportNothing_whenEverySecretIsProvidedAndStrong() {
        List<String> problems = SecretsValidator.collectProblems(
                STRONG_JWT, STRONG_ADMIN_PWD, "dbSecret123!", "vapidPub", "vapidPriv");

        assertThat(problems).isEmpty();
    }

    @Test
    void shouldRejectTheRepositoryJwtSecret() {
        List<String> problems = SecretsValidator.collectProblems(
                REPO_JWT, STRONG_ADMIN_PWD, "db", "pub", "priv");

        assertThat(problems).anyMatch(p -> p.contains("JWT_SECRET") && p.contains("valeur par défaut"));
    }

    @Test
    void shouldRejectTheRepositoryAdminPassword() {
        List<String> problems = SecretsValidator.collectProblems(
                STRONG_JWT, "Admin123!", "db", "pub", "priv");

        assertThat(problems).anyMatch(p -> p.contains("ADMIN_PASSWORD"));
    }

    @Test
    void shouldRejectTheRepositoryVapidKeys() {
        List<String> problems = SecretsValidator.collectProblems(STRONG_JWT, STRONG_ADMIN_PWD, "db",
                "BPLSJxSB3QAA1XsJVFED-6HGGyJmFios6ETEo9MJ-7RmbPOXioh_0WsOUPorDEpCbP_PaEJ3wGyo0t3f93y2sU4",
                "AM_NeFGDjso48CY-G965jKrTatAREp-TA-1KCPcJgbJI");

        assertThat(problems)
                .anyMatch(p -> p.contains("VAPID_PUBLIC_KEY"))
                .anyMatch(p -> p.contains("VAPID_PRIVATE_KEY"));
    }

    @Test
    void shouldRejectAJwtSecretTooShortForHs256() {
        List<String> problems = SecretsValidator.collectProblems(
                "trop-court", STRONG_ADMIN_PWD, "db", "pub", "priv");

        assertThat(problems).anyMatch(p -> p.contains("256 bits"));
    }

    @Test
    void shouldRejectAMissingRequiredSecret() {
        List<String> problems = SecretsValidator.collectProblems(
                STRONG_JWT, STRONG_ADMIN_PWD, "", "pub", "priv");

        assertThat(problems).contains("DB_PASSWORD n'est pas défini");
    }

    @Test
    void shouldRejectATooShortAdminPassword() {
        List<String> problems = SecretsValidator.collectProblems(
                STRONG_JWT, "court1!", "db", "pub", "priv");

        assertThat(problems).anyMatch(p -> p.contains("12 caractères"));
    }

    @Test
    void shouldTolerateDisabledPushNotifications() {
        // Sans clés VAPID, seules les notifications push sont indisponibles.
        List<String> problems = SecretsValidator.collectProblems(
                STRONG_JWT, STRONG_ADMIN_PWD, "db", null, null);

        assertThat(problems).isEmpty();
    }

    @Test
    void shouldReportEveryProblemAtOnce_notJustTheFirst() {
        // Un déploiement mal configuré doit voir la liste complète, pas la découvrir
        // secret par secret à chaque redémarrage.
        List<String> problems = SecretsValidator.collectProblems(REPO_JWT, "Admin123!", "", "pub", "priv");

        assertThat(problems).hasSizeGreaterThanOrEqualTo(3);
        assertThat(SecretsValidator.formatMessage(problems))
                .contains("JWT_SECRET").contains("ADMIN_PASSWORD").contains("DB_PASSWORD")
                .contains("DEPLOIEMENT.md");
    }

    // --- Déclenchement selon le profil ---

    private void run(MockEnvironment env) {
        new SecretsValidator().validate(env);
    }

    private MockEnvironment environmentWithRepositoryDefaults() {
        return new MockEnvironment()
                .withProperty("app.jwt.secret", REPO_JWT)
                .withProperty("app.admin.password", "Admin123!")
                .withProperty("spring.datasource.password", "");
    }

    @Test
    void shouldBlockStartup_onTheProdProfile() {
        MockEnvironment env = environmentWithRepositoryDefaults();
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> run(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DÉMARRAGE REFUSÉ");
    }

    @Test
    void shouldBlockStartup_whenProdIsDeclaredViaTheProperty() {
        // Cas du conteneur : SPRING_PROFILES_ACTIVE=prod, avant résolution des profils.
        MockEnvironment env = environmentWithRepositoryDefaults()
                .withProperty("spring.profiles.active", "prod");

        assertThatThrownBy(() -> run(env)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldStayOutOfTheWay_onDevelopmentAndTestProfiles() {
        // Les valeurs par défaut sont précisément là pour que le projet démarre sans
        // configuration en local : le contrôle ne doit pas s'y appliquer.
        MockEnvironment dev = environmentWithRepositoryDefaults();
        dev.setActiveProfiles("dev");

        assertThatCode(() -> run(dev)).doesNotThrowAnyException();
    }

    @Test
    void shouldNotBlock_whenProdSecretsAreProperlySet() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("app.jwt.secret", STRONG_JWT)
                .withProperty("app.admin.password", STRONG_ADMIN_PWD)
                .withProperty("spring.datasource.password", "dbSecret123!");
        env.setActiveProfiles("prod");

        assertThatCode(() -> run(env)).doesNotThrowAnyException();
    }
}
