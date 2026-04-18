package orga.takwa.ummati.dto.event;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AttendanceRequest(
        @NotNull List<UUID> userIds
) {}

