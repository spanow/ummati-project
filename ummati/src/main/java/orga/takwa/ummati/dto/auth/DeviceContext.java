package orga.takwa.ummati.dto.auth;

import orga.takwa.ummati.entity.enums.DevicePlatform;

/**
 * Origine d'une demande d'authentification.
 *
 * <p>Transporté par des en-têtes plutôt que par le corps des requêtes : le contexte
 * s'applique aussi bien au login qu'au refresh, et l'ajouter aux en-têtes laisse
 * {@link LoginRequest} — donc le contrat du client web existant — parfaitement
 * inchangé.
 *
 * <p>En l'absence d'en-tête, on retombe sur {@link #web()} : un client historique
 * conserve exactement le comportement d'avant.
 */
public record DeviceContext(
        DevicePlatform platform,
        String deviceId,
        String deviceName,
        String appVersion
) {

    public static final String HEADER_PLATFORM = "X-Device-Platform";
    public static final String HEADER_DEVICE_ID = "X-Device-Id";
    public static final String HEADER_DEVICE_NAME = "X-Device-Name";
    public static final String HEADER_APP_VERSION = "X-App-Version";

    private static final int MAX_ID_LENGTH = 100;
    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_VERSION_LENGTH = 20;

    public static DeviceContext web() {
        return new DeviceContext(DevicePlatform.WEB, null, null, null);
    }

    /**
     * Construit le contexte à partir d'en-têtes non fiables : tout ce qui arrive ici
     * vient du client. Une plateforme inconnue est ramenée à WEB (donc au régime de
     * session le plus court) et les champs libres sont tronqués aux largeurs des
     * colonnes, faute de quoi un en-tête trop long ferait échouer l'insertion et donc
     * la connexion elle-même.
     */
    public static DeviceContext fromHeaders(String platform, String deviceId,
                                            String deviceName, String appVersion) {
        DevicePlatform parsed = DevicePlatform.WEB;
        if (platform != null && !platform.isBlank()) {
            try {
                parsed = DevicePlatform.valueOf(platform.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Plateforme inconnue : on reste sur WEB plutôt que de refuser la connexion.
            }
        }
        return new DeviceContext(
                parsed,
                truncate(deviceId, MAX_ID_LENGTH),
                truncate(deviceName, MAX_NAME_LENGTH),
                truncate(appVersion, MAX_VERSION_LENGTH)
        );
    }

    public boolean isNative() {
        return platform != null && platform.isNative();
    }

    private static String truncate(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
