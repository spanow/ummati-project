package orga.takwa.ummati.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Règle de récurrence : répète le créneau principal (startDate/endDate de la requête)
 * jusqu'à {@code until} inclus. Ex. WEEKLY interval 1 = chaque semaine.
 */
public record RecurrenceInput(
        @NotBlank String frequency,   // WEEKLY | MONTHLY
        Integer interval,             // pas de répétition (défaut 1)
        @NotNull LocalDate until      // dernière date de créneau incluse
) {}
