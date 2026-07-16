package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.Report;
import orga.takwa.ummati.entity.enums.ReportStatus;
import orga.takwa.ummati.entity.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
            UUID reporterId, ReportTargetType targetType, UUID targetId, ReportStatus status);
}
