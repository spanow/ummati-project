package orga.takwa.ummati.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import orga.takwa.ummati.entity.EventSignup;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.EventSignupRepository;
import orga.takwa.ummati.repository.OrganizationRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Génère l'attestation de bénévolat (PDF) d'un utilisateur, en self-service.
 * Les heures proviennent des présences validées et stockées (jamais recalculées),
 * ce qui garantit un document stable dans le temps.
 */
@Service
public class AttestationService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    private final UserRepository userRepository;
    private final EventSignupRepository eventSignupRepository;
    private final OrganizationRepository organizationRepository;
    private final TemplateEngine templateEngine;

    public AttestationService(UserRepository userRepository, EventSignupRepository eventSignupRepository,
                              OrganizationRepository organizationRepository, TemplateEngine templateEngine) {
        this.userRepository = userRepository;
        this.eventSignupRepository = eventSignupRepository;
        this.organizationRepository = organizationRepository;
        this.templateEngine = templateEngine;
    }

    /** @param orgFilter si non null, restreint l'attestation à cette organisation. */
    @Transactional(readOnly = true)
    public byte[] generate(UUID userId, UUID orgFilter) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        List<EventSignup> signups = eventSignupRepository.findAttendedWithHoursByUserId(userId);
        if (orgFilter != null) {
            signups = signups.stream()
                    .filter(s -> s.getEvent().getOrganization().getId().equals(orgFilter))
                    .toList();
        }

        List<Map<String, Object>> lines = new ArrayList<>();
        Map<String, BigDecimal> perOrg = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (EventSignup s : signups) {
            BigDecimal h = s.getHoursValidated() != null ? s.getHoursValidated() : BigDecimal.ZERO;
            total = total.add(h);
            String orgName = s.getEvent().getOrganization().getName();
            perOrg.merge(orgName, h, BigDecimal::add);
            Map<String, Object> line = new HashMap<>();
            line.put("date", s.getOccurrence().getStartDate().format(DATE_FMT));
            line.put("title", s.getEvent().getTitle());
            line.put("orgName", orgName);
            line.put("hours", formatHours(h));
            lines.add(line);
        }

        String scopeLabel = orgFilter != null
                ? organizationRepository.findById(orgFilter).map(o -> o.getName()).orElse("l'organisation")
                : "l'ensemble de vos engagements sur Ummati";

        List<Map<String, Object>> orgTotals = new ArrayList<>();
        if (orgFilter == null) {
            perOrg.forEach((name, h) -> {
                Map<String, Object> m = new HashMap<>();
                m.put("orgName", name);
                m.put("hours", formatHours(h));
                orgTotals.add(m);
            });
        }

        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("volunteerName", (user.getFirstName() + " " + user.getLastName()).trim());
        ctx.setVariable("emittedAt", LocalDate.now().format(DATE_FMT));
        ctx.setVariable("scopeLabel", scopeLabel);
        ctx.setVariable("singleOrg", orgFilter != null);
        ctx.setVariable("lines", lines);
        ctx.setVariable("orgTotals", orgTotals);
        ctx.setVariable("totalHours", formatHours(total));
        ctx.setVariable("missionCount", lines.size());

        String html = templateEngine.process("attestation", ctx);
        return renderPdf(html);
    }

    // "3,5 h", "12 h", "0 h"
    private String formatHours(BigDecimal h) {
        if (h == null) h = BigDecimal.ZERO;
        h = h.setScale(2, RoundingMode.HALF_UP);
        String s = h.remainder(BigDecimal.ONE).signum() == 0
                ? h.toBigInteger().toString()
                : h.stripTrailingZeros().toPlainString().replace('.', ',');
        return s + " h";
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération de l'attestation PDF", e);
        }
    }
}
