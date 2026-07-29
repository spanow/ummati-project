package orga.takwa.ummati.controller.retention;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.notification.NotificationPreferencesResponse;
import orga.takwa.ummati.entity.NotificationPreferences;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.NotificationCategory;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.UserRepository;
import orga.takwa.ummati.service.NotificationPreferenceService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** Réglages d'envoi d'emails et désinscription en un clic. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Préférences de notification", description = "Ce que le bénévole accepte de recevoir")
public class NotificationPreferenceController {

    private static final String UNSUBSCRIBE_PAGE = """
            <!doctype html><html lang="fr"><head><meta charset="utf-8">
            <title>Désabonnement — Ummati</title></head>
            <body style="font-family:system-ui,sans-serif;max-width:520px;margin:80px auto;padding:0 24px;color:#1b2b28">
              <h1 style="font-size:1.4rem">C'est fait</h1>
              <p>Vous ne recevrez plus ce type d'email de la part d'Ummati.</p>
              <p style="color:#6b7a77;font-size:.9rem">
                Vous pouvez réactiver ces envois à tout moment depuis vos paramètres.
                Les messages liés à vos inscriptions continueront de vous parvenir.
              </p>
            </body></html>
            """;

    private final NotificationPreferenceService preferenceService;
    private final UserRepository userRepository;

    public NotificationPreferenceController(NotificationPreferenceService preferenceService,
                                            UserRepository userRepository) {
        this.preferenceService = preferenceService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Mes préférences d'email")
    @GetMapping("/profile/notification-preferences")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponse>> get(@CurrentUser UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        return ResponseEntity.ok(ApiResponse.ok(toResponse(preferenceService.getOrCreate(user))));
    }

    @Operation(summary = "Activer ou couper une catégorie d'email")
    @PutMapping("/profile/notification-preferences")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponse>> update(
            @CurrentUser UUID userId, @RequestBody Map<String, Boolean> body) {
        if (body == null || body.isEmpty()) {
            throw new BusinessRuleException("Aucune préférence fournie");
        }
        NotificationPreferences prefs = null;
        for (Map.Entry<String, Boolean> entry : body.entrySet()) {
            NotificationCategory category = parseCategory(entry.getKey());
            prefs = preferenceService.update(userId, category, Boolean.TRUE.equals(entry.getValue()));
        }
        return ResponseEntity.ok(ApiResponse.ok(toResponse(prefs)));
    }

    /**
     * Désinscription depuis un email, sans connexion.
     *
     * <p>Réponse HTML volontairement : ce lien s'ouvre dans un navigateur depuis une
     * boîte mail, il n'est pas appelé par le frontend. Toujours 200, même sur jeton
     * inconnu — répondre autrement en ferait un moyen de tester des jetons.
     */
    @Operation(summary = "Se désabonner en un clic depuis un email")
    @GetMapping(value = "/public/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unsubscribe(@RequestParam String token,
                                              @RequestParam(required = false) String category) {
        NotificationCategory parsed = null;
        if (category != null && !category.isBlank()) {
            try {
                parsed = NotificationCategory.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Catégorie inconnue : on coupe tout plutôt que de ne rien faire.
            }
        }
        preferenceService.unsubscribeByToken(token, parsed);
        return ResponseEntity.ok(UNSUBSCRIBE_PAGE);
    }

    private NotificationCategory parseCategory(String key) {
        NotificationCategory category;
        try {
            category = NotificationCategory.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Catégorie inconnue : " + key);
        }
        if (category == NotificationCategory.TRANSACTIONAL) {
            throw new BusinessRuleException(
                    "Les emails liés à vos inscriptions ne peuvent pas être désactivés");
        }
        return category;
    }

    private NotificationPreferencesResponse toResponse(NotificationPreferences p) {
        return new NotificationPreferencesResponse(
                p.isEmailNewMissions(), p.isEmailReminders(),
                p.isEmailMemberships(), p.isEmailAnnouncements());
    }
}
