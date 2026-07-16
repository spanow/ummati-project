package orga.takwa.ummati.dto.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import orga.takwa.ummati.entity.enums.ReportStatus;

public record ResolveReportRequest(
        @NotNull ReportStatus status,
        @Size(max = 1000) String resolutionNote
) {}
