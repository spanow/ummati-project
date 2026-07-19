package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.report.CreateReportRequest;
import orga.takwa.ummati.dto.report.ReportResponse;
import orga.takwa.ummati.dto.report.ResolveReportRequest;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.Report;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.ReportReason;
import orga.takwa.ummati.entity.enums.ReportStatus;
import orga.takwa.ummati.entity.enums.ReportTargetType;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.EventRepository;
import orga.takwa.ummati.repository.OrgAnnouncementRepository;
import orga.takwa.ummati.repository.OrganizationRepository;
import orga.takwa.ummati.repository.ReportRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private EventRepository eventRepository;
    @Mock private OrgAnnouncementRepository announcementRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private ReportService reportService;

    private UUID reporterId;
    private UUID orgId;
    private User reporter;

    @BeforeEach
    void setUp() {
        reporterId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        reporter = new User();
        reporter.setId(reporterId);
        reporter.setFirstName("Jean");
        reporter.setLastName("Dupont");
    }

    @Test
    void create_shouldSucceed_whenTargetExistsAndNoDuplicatePending() {
        when(organizationRepository.existsById(orgId)).thenReturn(true);
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporterId, ReportTargetType.ORGANIZATION, orgId, ReportStatus.PENDING)).thenReturn(false);
        when(userRepository.getReferenceById(reporterId)).thenReturn(reporter);
        when(reportRepository.save(any())).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        Organization org = new Organization();
        org.setName("Asso Solidarité");
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

        ReportResponse result = reportService.create(reporterId,
                new CreateReportRequest(ReportTargetType.ORGANIZATION, orgId, ReportReason.SPAM, "Contenu suspect"));

        assertThat(result.status()).isEqualTo(ReportStatus.PENDING);
        assertThat(result.reason()).isEqualTo(ReportReason.SPAM);
        assertThat(result.targetLabel()).isEqualTo("Asso Solidarité");
        verify(auditService).log(eq(reporterId), eq("REPORT_CREATED"), eq("Report"), any());
    }

    @Test
    void create_shouldFail_whenTargetDoesNotExist() {
        when(organizationRepository.existsById(orgId)).thenReturn(false);

        assertThatThrownBy(() -> reportService.create(reporterId,
                new CreateReportRequest(ReportTargetType.ORGANIZATION, orgId, ReportReason.SPAM, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void create_shouldFail_whenDuplicatePendingReportExists() {
        when(organizationRepository.existsById(orgId)).thenReturn(true);
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporterId, ReportTargetType.ORGANIZATION, orgId, ReportStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> reportService.create(reporterId,
                new CreateReportRequest(ReportTargetType.ORGANIZATION, orgId, ReportReason.FRAUD, null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void list_shouldFilterByStatus_whenProvided() {
        Report r = buildReport();
        when(reportRepository.findByStatusOrderByCreatedAtDesc(eq(ReportStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(r)));

        var results = reportService.list(ReportStatus.PENDING, Pageable.unpaged());

        assertThat(results.getContent()).hasSize(1);
        verify(reportRepository, never()).findAllByOrderByCreatedAtDesc(any());
    }

    @Test
    void list_shouldReturnAll_whenStatusNull() {
        when(reportRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildReport())));

        var results = reportService.list(null, Pageable.unpaged());

        assertThat(results.getContent()).hasSize(1);
    }

    @Test
    void resolve_shouldSucceed_andStampReviewer() {
        Report r = buildReport();
        UUID adminId = UUID.randomUUID();
        User admin = new User();
        admin.setId(adminId);
        admin.setFirstName("Admin");
        admin.setLastName("Platform");

        when(reportRepository.findById(r.getId())).thenReturn(Optional.of(r));
        when(userRepository.getReferenceById(adminId)).thenReturn(admin);
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(organizationRepository.findById(any())).thenReturn(Optional.empty());

        ReportResponse result = reportService.resolve(adminId, r.getId(),
                new ResolveReportRequest(ReportStatus.ACTION_TAKEN, "ONG suspendue"));

        assertThat(result.status()).isEqualTo(ReportStatus.ACTION_TAKEN);
        assertThat(result.reviewedByName()).isEqualTo("Admin Platform");
        verify(auditService).log(eq(adminId), eq("REPORT_ACTION_TAKEN"), eq("Report"), eq(r.getId()));
    }

    @Test
    void resolve_shouldFail_whenSettingBackToPending() {
        Report r = buildReport();
        UUID adminId = UUID.randomUUID();
        when(reportRepository.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> reportService.resolve(adminId, r.getId(),
                new ResolveReportRequest(ReportStatus.PENDING, null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void resolve_shouldFail_whenReportNotFound() {
        UUID reportId = UUID.randomUUID();
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.resolve(UUID.randomUUID(), reportId,
                new ResolveReportRequest(ReportStatus.DISMISSED, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Report buildReport() {
        Report r = new Report();
        r.setId(UUID.randomUUID());
        r.setReporter(reporter);
        r.setTargetType(ReportTargetType.ORGANIZATION);
        r.setTargetId(orgId);
        r.setReason(ReportReason.SPAM);
        r.setStatus(ReportStatus.PENDING);
        return r;
    }
}
