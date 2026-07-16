package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.report.CreateReportRequest;
import orga.takwa.ummati.dto.report.ReportResponse;
import orga.takwa.ummati.dto.report.ResolveReportRequest;
import orga.takwa.ummati.entity.OrgAnnouncement;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.Report;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.ReportStatus;
import orga.takwa.ummati.entity.enums.ReportTargetType;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.EventRepository;
import orga.takwa.ummati.repository.OrgAnnouncementRepository;
import orga.takwa.ummati.repository.OrganizationRepository;
import orga.takwa.ummati.repository.ReportRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final EventRepository eventRepository;
    private final OrgAnnouncementRepository announcementRepository;
    private final AuditService auditService;

    public ReportService(ReportRepository reportRepository,
                          UserRepository userRepository,
                          OrganizationRepository organizationRepository,
                          EventRepository eventRepository,
                          OrgAnnouncementRepository announcementRepository,
                          AuditService auditService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.eventRepository = eventRepository;
        this.announcementRepository = announcementRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ReportResponse create(UUID reporterId, CreateReportRequest request) {
        if (!targetExists(request.targetType(), request.targetId())) {
            throw new ResourceNotFoundException("L'élément signalé n'existe pas ou plus");
        }
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporterId, request.targetType(), request.targetId(), ReportStatus.PENDING)) {
            throw new BusinessRuleException("Vous avez déjà signalé cet élément, il est en cours d'examen");
        }

        User reporter = userRepository.getReferenceById(reporterId);
        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(request.targetType());
        report.setTargetId(request.targetId());
        report.setReason(request.reason());
        report.setDescription(request.description() != null ? request.description().trim() : null);
        report = reportRepository.save(report);

        auditService.log(reporterId, "REPORT_CREATED", "Report", report.getId());
        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> list(ReportStatus status, Pageable pageable) {
        Page<Report> page = status != null
                ? reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : reportRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public ReportResponse resolve(UUID adminId, UUID reportId, ResolveReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement non trouvé"));
        if (request.status() == ReportStatus.PENDING) {
            throw new BusinessRuleException("Un signalement ne peut pas être remis en attente");
        }

        report.setStatus(request.status());
        report.setResolutionNote(request.resolutionNote() != null ? request.resolutionNote().trim() : null);
        report.setReviewedBy(userRepository.getReferenceById(adminId));
        report.setReviewedAt(LocalDateTime.now());
        report = reportRepository.save(report);

        auditService.log(adminId, "REPORT_" + request.status().name(), "Report", reportId);
        return toResponse(report);
    }

    private boolean targetExists(ReportTargetType type, UUID targetId) {
        return switch (type) {
            case ORGANIZATION -> organizationRepository.existsById(targetId);
            case EVENT -> eventRepository.existsById(targetId);
            case ORG_ANNOUNCEMENT -> announcementRepository.existsById(targetId);
        };
    }

    private String resolveTargetLabel(ReportTargetType type, UUID targetId) {
        try {
            return switch (type) {
                case ORGANIZATION -> organizationRepository.findById(targetId)
                        .map(Organization::getName).orElse("(supprimé)");
                case EVENT -> eventRepository.findById(targetId)
                        .map(orga.takwa.ummati.entity.Event::getTitle).orElse("(supprimé)");
                case ORG_ANNOUNCEMENT -> announcementRepository.findById(targetId)
                        .map(OrgAnnouncement::getTitle).orElse("(supprimé)");
            };
        } catch (Exception e) {
            return "(inconnu)";
        }
    }

    private ReportResponse toResponse(Report r) {
        User reporter = r.getReporter();
        User reviewer = r.getReviewedBy();
        return new ReportResponse(
                r.getId(),
                reporter.getId(),
                reporter.getFirstName() + " " + reporter.getLastName(),
                r.getTargetType(),
                r.getTargetId(),
                resolveTargetLabel(r.getTargetType(), r.getTargetId()),
                r.getReason(),
                r.getDescription(),
                r.getStatus(),
                r.getResolutionNote(),
                reviewer != null ? reviewer.getFirstName() + " " + reviewer.getLastName() : null,
                r.getReviewedAt(),
                r.getCreatedAt()
        );
    }
}
