package orga.takwa.ummati.dto.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import orga.takwa.ummati.entity.enums.ReportReason;
import orga.takwa.ummati.entity.enums.ReportTargetType;

import java.util.UUID;

public record CreateReportRequest(
        @NotNull ReportTargetType targetType,
        @NotNull UUID targetId,
        @NotNull ReportReason reason,
        @Size(max = 1000) String description
) {}
