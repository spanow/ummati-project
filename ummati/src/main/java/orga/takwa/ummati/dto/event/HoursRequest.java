package orga.takwa.ummati.dto.event;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Ajustement des heures certifiées d'une présence par l'ONG. */
public record HoursRequest(@NotNull BigDecimal hours) {}
