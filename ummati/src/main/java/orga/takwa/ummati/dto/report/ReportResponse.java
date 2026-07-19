package orga.takwa.ummati.dto.report;

import orga.takwa.ummati.entity.enums.ReportReason;
import orga.takwa.ummati.entity.enums.ReportStatus;
import orga.takwa.ummati.entity.enums.ReportTargetType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        UUID reporterId,
        String reporterName,
        ReportTargetType targetType,
        UUID targetId,
        String targetLabel,
        ReportReason reason,
        String description,
        ReportStatus status,
        String resolutionNote,
        String reviewedByName,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {}
