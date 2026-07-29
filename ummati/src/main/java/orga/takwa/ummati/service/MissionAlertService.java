package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.alert.MissionAlertRequest;
import orga.takwa.ummati.dto.alert.MissionAlertResponse;
import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.MissionAlert;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.AlertFrequency;
import orga.takwa.ummati.entity.enums.EventStatus;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.EventRepository;
import orga.takwa.ummati.repository.MissionAlertRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Alertes missions : une recherche que le bénévole enregistre et qu'on rejoue pour lui.
 *
 * <p>C'est le seul mécanisme de rétention qui va au-devant du bénévole. Les favoris et
 * le suivi d'ONG supposent qu'il revienne ; une alerte lui écrit quand quelque chose
 * correspond enfin à ce qu'il cherchait.
 */
@Service
public class MissionAlertService {

    /** Au-delà, la gestion devient illisible et l'envoi ressemble à du publipostage. */
    static final int MAX_ALERTS_PER_USER = 5;

    private static final int MAX_RADIUS_KM = 500;

    /** Missions listées dans un email : au-delà on renvoie vers la recherche. */
    static final int MAX_MATCHES_PER_ALERT = 5;

    private final MissionAlertRepository alertRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public MissionAlertService(MissionAlertRepository alertRepository,
                               EventRepository eventRepository,
                               UserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<MissionAlertResponse> listMine(UUID userId) {
        return alertRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(MissionAlertService::toResponse)
                .toList();
    }

    @Transactional
    public MissionAlertResponse create(UUID userId, MissionAlertRequest request) {
        if (alertRepository.countByUserId(userId) >= MAX_ALERTS_PER_USER) {
            throw new BusinessRuleException(
                    "Vous ne pouvez pas dépasser " + MAX_ALERTS_PER_USER + " alertes. "
                            + "Supprimez-en une pour en créer une nouvelle.");
        }
        validate(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        MissionAlert alert = new MissionAlert();
        alert.setUser(user);
        apply(alert, request);
        // Point de départ = maintenant : une alerte neuve ne rejoue pas l'historique.
        alert.setLastSentAt(LocalDateTime.now());
        return toResponse(alertRepository.save(alert));
    }

    @Transactional
    public MissionAlertResponse update(UUID userId, UUID alertId, MissionAlertRequest request) {
        validate(request);
        MissionAlert alert = requireOwned(userId, alertId);
        apply(alert, request);
        return toResponse(alertRepository.save(alert));
    }

    @Transactional
    public void delete(UUID userId, UUID alertId) {
        alertRepository.delete(requireOwned(userId, alertId));
    }

    /**
     * Nouvelles missions correspondant à l'alerte depuis son dernier envoi.
     *
     * <p>Le filtrage géographique réutilise la même formule que la recherche « autour
     * de moi » — deux implémentations divergeraient tôt ou tard et le bénévole
     * recevrait des missions que la recherche ne lui montre pas.
     */
    @Transactional(readOnly = true)
    public List<Event> findMatches(MissionAlert alert, LocalDateTime now) {
        LocalDateTime since = alert.getLastSentAt() != null
                ? alert.getLastSentAt()
                : now.minusDays(alert.getFrequency() == AlertFrequency.DAILY ? 1 : 7);

        Set<String> domains = alert.domainSet();
        Set<String> types = alert.typeSet();

        // Nouveauté et fenêtre temporelle filtrées en base ; seuls les critères propres
        // à l'alerte (domaines, types, périmètre) sont appliqués sur le reliquat.
        return eventRepository.findPublishedSince(since, now).stream()
                .filter(e -> types.isEmpty() || types.contains(e.getType().name()))
                .filter(e -> domains.isEmpty()
                        || domains.contains(e.getOrganization().getDomain().name()))
                .filter(e -> matchesLocation(alert, e))
                .sorted((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                .limit(MAX_MATCHES_PER_ALERT)
                .toList();
    }

    private boolean matchesLocation(MissionAlert alert, Event event) {
        if (alert.hasRadius()) {
            if (event.getLocationLat() == null || event.getLocationLng() == null) {
                // Une mission sans coordonnées ne peut pas être située : hors périmètre.
                return false;
            }
            double distance = orga.takwa.ummati.repository.EventSpecification.distanceKm(
                    alert.getLat().doubleValue(), alert.getLng().doubleValue(),
                    event.getLocationLat().doubleValue(), event.getLocationLng().doubleValue());
            return distance <= alert.getRadiusKm();
        }
        if (alert.getCity() != null && !alert.getCity().isBlank()) {
            return event.getLocationCity() != null
                    && event.getLocationCity().equalsIgnoreCase(alert.getCity().trim());
        }
        return true;
    }

    @Transactional
    public void markSent(MissionAlert alert, LocalDateTime when) {
        alert.setLastSentAt(when);
        alertRepository.save(alert);
    }

    // --- Helpers ---

    private MissionAlert requireOwned(UUID userId, UUID alertId) {
        MissionAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte non trouvée"));
        if (!alert.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Cette alerte ne vous appartient pas");
        }
        return alert;
    }

    private void validate(MissionAlertRequest request) {
        boolean hasCoords = request.lat() != null && request.lng() != null;
        if (hasCoords != (request.radiusKm() != null)) {
            throw new BusinessRuleException(
                    "Un périmètre exige à la fois des coordonnées et un rayon");
        }
        if (request.radiusKm() != null
                && (request.radiusKm() <= 0 || request.radiusKm() > MAX_RADIUS_KM)) {
            throw new BusinessRuleException(
                    "Le rayon doit être compris entre 1 et " + MAX_RADIUS_KM + " km");
        }
        if (hasCoords && (Math.abs(request.lat().doubleValue()) > 90
                || Math.abs(request.lng().doubleValue()) > 180)) {
            throw new BusinessRuleException("Coordonnées hors limites");
        }
    }

    private void apply(MissionAlert alert, MissionAlertRequest request) {
        alert.setLabel(request.label().trim());
        alert.setCity(blankToNull(request.city()));
        alert.setLat(request.lat());
        alert.setLng(request.lng());
        alert.setRadiusKm(request.radiusKm());
        alert.setDomains(joinCsv(request.domains()));
        alert.setTypes(joinCsv(request.types()));
        alert.setFrequency(request.frequency() != null ? request.frequency() : AlertFrequency.WEEKLY);
        alert.setEnabled(request.enabled() == null || request.enabled());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String joinCsv(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(",", values);
    }

    static MissionAlertResponse toResponse(MissionAlert alert) {
        return new MissionAlertResponse(
                alert.getId(), alert.getLabel(), alert.getCity(),
                alert.getLat(), alert.getLng(), alert.getRadiusKm(),
                List.copyOf(alert.domainSet()), List.copyOf(alert.typeSet()),
                alert.getFrequency().name(), alert.isEnabled(),
                alert.getLastSentAt(), alert.getCreatedAt());
    }

    /** Exposé pour les tests : conversion d'un rayon en BigDecimal sans perte. */
    static BigDecimal coordinate(double value) {
        return BigDecimal.valueOf(value);
    }
}
