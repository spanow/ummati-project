package orga.takwa.ummati.dto.auth;

import orga.takwa.ummati.entity.enums.DevicePlatform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les en-têtes device viennent du client et ne sont donc pas fiables. Ce qui est
 * vérifié ici, c'est qu'aucune valeur mal formée ne peut faire échouer une connexion.
 */
class DeviceContextTest {

    @Test
    @DisplayName("Sans en-tête, on retombe sur le régime web historique")
    void missingHeadersFallBackToWeb() {
        DeviceContext context = DeviceContext.fromHeaders(null, null, null, null);

        assertThat(context.platform()).isEqualTo(DevicePlatform.WEB);
        assertThat(context.isNative()).isFalse();
    }

    @Test
    @DisplayName("La plateforme est reconnue quelle que soit la casse")
    void platformParsingIsCaseInsensitive() {
        assertThat(DeviceContext.fromHeaders("ios", null, null, null).platform())
                .isEqualTo(DevicePlatform.IOS);
        assertThat(DeviceContext.fromHeaders("  Android  ", null, null, null).platform())
                .isEqualTo(DevicePlatform.ANDROID);
    }

    @Test
    @DisplayName("Une plateforme inconnue ne fait pas échouer la connexion, elle dégrade vers WEB")
    void unknownPlatformDegradesToWeb() {
        DeviceContext context = DeviceContext.fromHeaders("windows-phone", null, null, null);

        assertThat(context.platform()).isEqualTo(DevicePlatform.WEB);
        assertThat(context.isNative()).isFalse();
    }

    /**
     * Sans troncature, un en-tête trop long provoquerait une erreur d'insertion — donc
     * un échec de connexion — pour une simple métadonnée d'affichage.
     */
    @Test
    @DisplayName("Les champs libres sont tronqués à la largeur des colonnes")
    void oversizedFieldsAreTruncated() {
        DeviceContext context = DeviceContext.fromHeaders(
                "IOS", "x".repeat(500), "y".repeat(500), "z".repeat(500));

        assertThat(context.deviceId()).hasSize(100);
        assertThat(context.deviceName()).hasSize(120);
        assertThat(context.appVersion()).hasSize(20);
    }

    @Test
    @DisplayName("Les chaînes vides sont normalisées en null")
    void blankFieldsBecomeNull() {
        DeviceContext context = DeviceContext.fromHeaders("IOS", "  ", "", null);

        assertThat(context.deviceId()).isNull();
        assertThat(context.deviceName()).isNull();
    }

    @Test
    @DisplayName("Seules les plateformes installées ouvrent droit à une session longue")
    void onlyInstalledPlatformsAreNative() {
        assertThat(DeviceContext.fromHeaders("IOS", null, null, null).isNative()).isTrue();
        assertThat(DeviceContext.fromHeaders("ANDROID", null, null, null).isNative()).isTrue();
        assertThat(DeviceContext.web().isNative()).isFalse();
    }
}
