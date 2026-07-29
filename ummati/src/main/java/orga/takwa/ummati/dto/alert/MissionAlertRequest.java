package orga.takwa.ummati.dto.alert;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import orga.takwa.ummati.entity.enums.AlertFrequency;

import java.math.BigDecimal;
import java.util.List;

/**
 * Création ou modification d'une alerte missions.
 *
 * <p>Le périmètre est soit une ville, soit un couple coordonnées + rayon. Les listes
 * vides valent « tous les domaines » et « tous les types ».
 */
public record MissionAlertRequest(
        @NotBlank @Size(max = 120) String label,
        @Size(max = 100) String city,
        BigDecimal lat,
        BigDecimal lng,
        Integer radiusKm,
        List<String> domains,
        List<String> types,
        AlertFrequency frequency,
        Boolean enabled
) {}
