package orga.takwa.ummati.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromEmail;
    private final String senderName;
    private final String baseUrl;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine,
                        @Value("${app.mail.from}") String fromEmail,
                        @Value("${app.mail.sender-name}") String senderName,
                        @Value("${app.base-url}") String baseUrl) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromEmail = fromEmail;
        this.senderName = senderName;
        this.baseUrl = baseUrl;
    }

    @Async
    public void sendVerificationEmail(String to, String firstName, String token) {
        sendTemplateEmail(to, "Confirmez votre email — Ummati", "email/verify",
                Map.of("firstName", firstName,
                       "verificationUrl", baseUrl + "/api/v1/auth/confirm-email?token=" + token));
    }

    @Async
    public void sendWelcomeEmail(String to, String firstName) {
        sendTemplateEmail(to, "Bienvenue sur Ummati !", "email/welcome",
                Map.of("firstName", firstName, "loginUrl", baseUrl + "/login"));
    }

    @Async
    public void sendPasswordResetEmail(String to, String firstName, String token) {
        sendTemplateEmail(to, "Réinitialisation de votre mot de passe — Ummati", "email/password-reset",
                Map.of("firstName", firstName,
                       "resetUrl", baseUrl + "/reset-password?token=" + token));
    }

    @Async
    public void sendPasswordChangedEmail(String to, String firstName) {
        sendTemplateEmail(to, "Mot de passe modifié — Ummati", "email/password-changed",
                Map.of("firstName", firstName));
    }

    @Async
    public void sendNotificationEmail(String to, String firstName, String title, String message, String link) {
        // Generic notification email - reuses welcome template structure
        Map<String, Object> vars = new java.util.HashMap<>();
        vars.put("firstName", firstName);
        vars.put("title", title);
        vars.put("message", message);
        if (link != null) vars.put("actionUrl", baseUrl + link);
        sendTemplateEmail(to, title + " — Ummati", "email/notification", vars);
    }

    private void sendTemplateEmail(String to, String subject, String template, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            context.setVariable("appName", senderName);
            String htmlContent = templateEngine.process(template, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, senderName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.debug("Email sent to {} - subject: {}", to, subject);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}

