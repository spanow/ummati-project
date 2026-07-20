package orga.takwa.ummati.controller.event;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.PageResponse;
import orga.takwa.ummati.dto.event.*;
import orga.takwa.ummati.service.EventService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Événements", description = "Gestion des événements, inscriptions et feedbacks")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // List all events of an org (all statuses) — org admin only
    @GetMapping("/organizations/{orgId}/events")
    public ResponseEntity<ApiResponse<PageResponse<EventSummary>>> listOrgEvents(
            @CurrentUser UUID userId, @PathVariable UUID orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate"));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                eventService.listOrgEvents(userId, orgId, pageable))));
    }

    // T-070: Create event
    @PostMapping("/organizations/{orgId}/events")
    public ResponseEntity<ApiResponse<EventDetail>> createEvent(
            @CurrentUser UUID userId, @PathVariable UUID orgId,
            @Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(eventService.createEvent(userId, orgId, request)));
    }

    // T-071: Update event
    @PutMapping("/events/{id}")
    public ResponseEntity<ApiResponse<EventDetail>> updateEvent(
            @CurrentUser UUID userId, @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.updateEvent(userId, id, request)));
    }

    // T-072: Change event status
    @PatchMapping("/events/{id}/status")
    public ResponseEntity<ApiResponse<EventDetail>> changeStatus(
            @CurrentUser UUID userId, @PathVariable UUID id,
            @Valid @RequestBody EventStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.changeStatus(userId, id, request)));
    }

    // T-073: List events (public)
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<PageResponse<EventSummary>>> listEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) UUID orgId,
            @RequestParam(required = false) Boolean online,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(required = false) UUID skillId,
            @RequestParam(defaultValue = "startDate,asc") String sort) {
        String[] sortParts = sort.split(",");
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])
                        ? Sort.Direction.DESC : Sort.Direction.ASC, sortParts[0]));
        var result = eventService.listEvents(type, city, orgId, online, from, to, skillId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    // T-074: Get event detail (public)
    @GetMapping("/events/{id}")
    public ResponseEntity<ApiResponse<EventDetail>> getEvent(
            @CurrentUser UUID userId, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getEvent(id, userId)));
    }

    // Get current user's signup status for an event
    @GetMapping("/events/{id}/signups/me")
    public ResponseEntity<ApiResponse<SignupResponse>> getMySignup(
            @CurrentUser UUID userId, @PathVariable UUID id) {
        return eventService.getMySignup(userId, id)
                .map(s -> ResponseEntity.ok(ApiResponse.ok(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    // T-075: Signup for event
    @PostMapping("/events/{id}/signups")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @CurrentUser UUID userId, @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(eventService.signup(userId, id)));
    }

    // T-076: Cancel signup
    @DeleteMapping("/events/{id}/signups")
    public ResponseEntity<Void> cancelSignup(
            @CurrentUser UUID userId, @PathVariable UUID id) {
        eventService.cancelSignup(userId, id);
        return ResponseEntity.noContent().build();
    }

    // T-077: List signups (admin ONG)
    @GetMapping("/events/{id}/signups")
    public ResponseEntity<ApiResponse<PageResponse<SignupResponse>>> listSignups(
            @CurrentUser UUID userId, @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        var result = eventService.listSignups(userId, id, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    // T-078: Export signups CSV (admin ONG)
    @GetMapping(value = "/events/{id}/signups/export", produces = "text/csv")
    public ResponseEntity<String> exportSignupsCsv(
            @CurrentUser UUID userId, @PathVariable UUID id) {
        String csv = eventService.exportSignupsCsv(userId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=signups.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // T-079: Mark attendance (admin ONG)
    @PatchMapping("/events/{id}/signups/attendance")
    public ResponseEntity<Void> markAttendance(
            @CurrentUser UUID userId, @PathVariable UUID id,
            @Valid @RequestBody AttendanceRequest request) {
        eventService.markAttendance(userId, id, request);
        return ResponseEntity.ok().build();
    }

    // ===== Créneaux (occurrences) — événements multi-créneaux / récurrents =====

    // Inscription à un créneau précis
    @PostMapping("/events/{id}/occurrences/{occId}/signups")
    public ResponseEntity<ApiResponse<SignupResponse>> signupToOccurrence(
            @CurrentUser UUID userId, @PathVariable UUID id, @PathVariable UUID occId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(eventService.signupToOccurrence(userId, id, occId)));
    }

    // Désinscription d'un créneau précis
    @DeleteMapping("/events/{id}/occurrences/{occId}/signups")
    public ResponseEntity<Void> cancelOccurrenceSignup(
            @CurrentUser UUID userId, @PathVariable UUID id, @PathVariable UUID occId) {
        eventService.cancelOccurrenceSignup(userId, id, occId);
        return ResponseEntity.noContent().build();
    }

    // Liste des inscrits d'un créneau (admin ONG)
    @GetMapping("/events/{id}/occurrences/{occId}/signups")
    public ResponseEntity<ApiResponse<PageResponse<SignupResponse>>> listOccurrenceSignups(
            @CurrentUser UUID userId, @PathVariable UUID id, @PathVariable UUID occId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        var result = eventService.listOccurrenceSignups(userId, id, occId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    // Marquage de présence sur un créneau (admin ONG)
    @PatchMapping("/events/{id}/occurrences/{occId}/signups/attendance")
    public ResponseEntity<Void> markOccurrenceAttendance(
            @CurrentUser UUID userId, @PathVariable UUID id, @PathVariable UUID occId,
            @Valid @RequestBody AttendanceRequest request) {
        eventService.markOccurrenceAttendance(userId, id, occId, request);
        return ResponseEntity.ok().build();
    }

    // Annuler / compléter un créneau précis (admin ONG)
    @PatchMapping("/events/{id}/occurrences/{occId}/status")
    public ResponseEntity<ApiResponse<EventDetail>> changeOccurrenceStatus(
            @CurrentUser UUID userId, @PathVariable UUID id, @PathVariable UUID occId,
            @Valid @RequestBody EventStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                eventService.changeOccurrenceStatus(userId, id, occId, request)));
    }

    // Ajuster les heures certifiées d'une présence (admin ONG)
    @PatchMapping("/events/{id}/occurrences/{occId}/signups/{signupId}/hours")
    public ResponseEntity<ApiResponse<SignupResponse>> adjustHours(
            @CurrentUser UUID userId, @PathVariable UUID id, @PathVariable UUID occId,
            @PathVariable UUID signupId, @Valid @RequestBody HoursRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                eventService.adjustSignupHours(userId, id, occId, signupId, request.hours())));
    }

    // Marquer des inscrits absents (admin ONG) — REGISTERED → NO_SHOW
    @PatchMapping("/events/{id}/occurrences/{occId}/signups/no-show")
    public ResponseEntity<Void> markNoShow(
            @CurrentUser UUID userId, @PathVariable UUID id, @PathVariable UUID occId,
            @Valid @RequestBody AttendanceRequest request) {
        eventService.markNoShow(userId, id, occId, request);
        return ResponseEntity.ok().build();
    }

    // T-080: Create feedback
    @PostMapping("/events/{id}/feedbacks")
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(
            @CurrentUser UUID userId, @PathVariable UUID id,
            @Valid @RequestBody CreateFeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(eventService.createFeedback(userId, id, request)));
    }

    // T-081: List feedbacks (public)
    @GetMapping("/events/{id}/feedbacks")
    public ResponseEntity<ApiResponse<FeedbackListResponse>> listFeedbacks(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        var feedbacks = eventService.listFeedbacks(id, pageable);
        Double avgRating = eventService.getAverageRating(id);
        var response = new FeedbackListResponse(PageResponse.from(feedbacks), avgRating);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    public record FeedbackListResponse(PageResponse<FeedbackResponse> feedbacks, Double averageRating) {}
}
