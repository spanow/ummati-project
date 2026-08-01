package orga.takwa.ummati.dto.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import orga.takwa.ummati.entity.enums.DevicePlatform;

/**
 * Enregistrement d'une installation native auprès du service de notifications.
 *
 * <p>Les longueurs maximales reprennent celles des colonnes : mieux vaut un 400
 * explicite qu'une erreur d'insertion en base.
 */
public record RegisterDeviceRequest(
        @NotBlank @Size(max = 512) String token,
        @NotNull DevicePlatform platform,
        @Size(max = 100) String deviceId,
        @Size(max = 120) String deviceName,
        @Size(max = 20) String appVersion
) {}
