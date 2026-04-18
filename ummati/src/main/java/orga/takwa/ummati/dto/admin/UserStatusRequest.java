package orga.takwa.ummati.dto.admin;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(@NotNull Boolean enabled) {}

