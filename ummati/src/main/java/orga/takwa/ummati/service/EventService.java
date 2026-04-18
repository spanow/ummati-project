package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.event.*;
import orga.takwa.ummati.entity.*;
import orga.takwa.ummati.entity.enums.*;
import orga.takwa.ummati.exception.*;
import orga.takwa.ummati.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventSignupRepository eventSignupRepository;
    private final EventFeedbackRepository eventFeedbackRepository;
    private final OrganizationService organizationService;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final MembershipRepository membershipRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;

    public EventService(EventRepository eventRepository, EventSignupRepository eventSignupRepository,
                        EventFeedbackRepository eventFeedbackRepository, OrganizationService organizationService,
                        UserRepository userRepository, SkillRepository skillRepository,
                        MembershipRepository membershipRepository, NotificationRepository notificationRepository,
                        AuditLogRepository auditLogRepository) {
        this.eventRepository = eventRepository;
        this.eventSignupRepository = eventSignupRepository;
        this.eventFeedbackRepository = eventFeedbackRepository;
        this.organizationService = organizationService;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.membershipRepository = membershipRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // T-070: Create event
    @Transactional
    public EventDetail createEvent(UUID userId, UUID orgId, CreateEventRequest request) {
        organizationService.verifyAdmin(userId, orgId);
        Organization org = organizationService.findOrg(orgId);

        if (org.getStatus() != OrganizationStatus.ACTIVE) {
            throw new BusinessRuleException("Seule une ONG active peut créer des événements");
        }

        validateDates(request.startDate(), request.endDate(), request.registrationDeadline());

        Event event = new Event();
        event.setOrganization(org);
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setObjectives(request.objectives());
        event.setType(EventType.valueOf(request.type()));
        event.setLocationName(request.locationName());
        event.setLocationAddress(request.locationAddress());
        event.setLocationCity(request.locationCity());
        event.setLocationZip(request.locationZip());
        event.setLocationLat(request.locationLat());
        event.setLocationLng(request.locationLng());
        event.setOnline(request.online());
        event.setOnlineLink(request.onlineLink());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setRegistrationDeadline(request.registrationDeadline());
        event.setMaxParticipants(request.maxParticipants());
        event.setMinAge(request.minAge());
        event.setStatus(EventStatus.DRAFT);
        event.setCreatedBy(userRepository.getReferenceById(userId));

        if (request.requiredSkillIds() != null && !request.requiredSkillIds().isEmpty()) {
            Set<Skill> skills = new HashSet<>(skillRepository.findAllById(request.requiredSkillIds()));
            event.setRequiredSkills(skills);
        }

        event = eventRepository.save(event);
        audit(userId, "EVENT_CREATED", "Event", event.getId());
        return toDetail(event);
    }

    // T-071: Update event
    @Transactional
    public EventDetail updateEvent(UUID userId, UUID eventId, UpdateEventRequest request) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());

        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) {
            throw new BusinessRuleException("Impossible de modifier un événement " + event.getStatus().name().toLowerCase());
        }

        if (event.getStatus() == EventStatus.PUBLISHED) {
            if (request.startDate() != null || request.endDate() != null || request.maxParticipants() != null) {
                throw new BusinessRuleException("Impossible de modifier les dates ou le nombre max de participants d'un événement publié");
            }
        }

        if (request.title() != null) event.setTitle(request.title().trim());
        if (request.description() != null) event.setDescription(request.description());
        if (request.objectives() != null) event.setObjectives(request.objectives());
        if (request.type() != null) event.setType(EventType.valueOf(request.type()));
        if (request.locationName() != null) event.setLocationName(request.locationName());
        if (request.locationAddress() != null) event.setLocationAddress(request.locationAddress());
        if (request.locationCity() != null) event.setLocationCity(request.locationCity());
        if (request.locationZip() != null) event.setLocationZip(request.locationZip());
        if (request.locationLat() != null) event.setLocationLat(request.locationLat());
        if (request.locationLng() != null) event.setLocationLng(request.locationLng());
        if (request.online() != null) event.setOnline(request.online());
        if (request.onlineLink() != null) event.setOnlineLink(request.onlineLink());
        if (request.startDate() != null) event.setStartDate(request.startDate());
        if (request.endDate() != null) event.setEndDate(request.endDate());
        if (request.registrationDeadline() != null) event.setRegistrationDeadline(request.registrationDeadline());
        if (request.maxParticipants() != null) event.setMaxParticipants(request.maxParticipants());
        if (request.minAge() != null) event.setMinAge(request.minAge());
        if (request.requiredSkillIds() != null) {
            Set<Skill> skills = new HashSet<>(skillRepository.findAllById(request.requiredSkillIds()));
            event.setRequiredSkills(skills);
        }

        event = eventRepository.save(event);
        audit(userId, "EVENT_UPDATED", "Event", eventId);
        return toDetail(event);
    }

    // T-072: Change event status
    @Transactional
    public EventDetail changeStatus(UUID userId, UUID eventId, EventStatusRequest request) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());

        String action = request.status().toUpperCase();

        switch (action) {
            case "PUBLISH" -> {
                if (event.getStatus() != EventStatus.DRAFT) {
                    throw new BusinessRuleException("Seul un événement en brouillon peut être publié");
                }
                event.setStatus(EventStatus.PUBLISHED);
                // Notify all active members of the org
                List<Membership> activeMembers = membershipRepository.findByOrganizationIdAndRoleAndStatus(
                        event.getOrganization().getId(), MembershipRole.MEMBER, MembershipStatus.ACTIVE);
                List<Membership> activeAdmins = membershipRepository.findByOrganizationIdAndRoleAndStatus(
                        event.getOrganization().getId(), MembershipRole.ADMIN, MembershipStatus.ACTIVE);
                var allMembers = new ArrayList<>(activeMembers);
                allMembers.addAll(activeAdmins);
                for (Membership m : allMembers) {
                    createNotification(m.getUser(), NotificationType.EVENT_PUBLISHED,
                            "Nouvel événement", "'" + event.getTitle() + "' par " + event.getOrganization().getName(),
                            "/events/" + event.getId());
                }
                audit(userId, "EVENT_PUBLISHED", "Event", eventId);
            }
            case "CANCEL" -> {
                if (request.reason() == null || request.reason().length() < 10) {
                    throw new BusinessRuleException("Le motif d'annulation doit faire au moins 10 caractères");
                }
                event.setStatus(EventStatus.CANCELLED);
                event.setCancellationReason(request.reason());
                // Notify all registered/waitlisted signups
                List<EventSignup> signups = eventSignupRepository.findByEventIdAndStatusIn(
                        eventId, List.of(SignupStatus.REGISTERED, SignupStatus.WAITLISTED));
                for (EventSignup s : signups) {
                    s.setStatus(SignupStatus.CANCELLED);
                    s.setCancelledAt(LocalDateTime.now());
                    createNotification(s.getUser(), NotificationType.EVENT_CANCELLED,
                            "Événement annulé", "'" + event.getTitle() + "' a été annulé. Motif : " + request.reason(),
                            "/events/" + event.getId());
                }
                eventSignupRepository.saveAll(signups);
                audit(userId, "EVENT_CANCELLED", "Event", eventId);
            }
            case "COMPLETE" -> {
                if (event.getStatus() != EventStatus.PUBLISHED) {
                    throw new BusinessRuleException("Seul un événement publié peut être marqué comme terminé");
                }
                event.setStatus(EventStatus.COMPLETED);
                audit(userId, "EVENT_COMPLETED", "Event", eventId);
            }
            default -> throw new BusinessRuleException("Action invalide. Utilisez PUBLISH, CANCEL ou COMPLETE.");
        }

        event = eventRepository.save(event);
        return toDetail(event);
    }

    // T-073: List events
    @Transactional(readOnly = true)
    public Page<EventSummary> listEvents(String type, String city, UUID orgId, Boolean online,
                                          LocalDateTime startAfter, LocalDateTime startBefore,
                                          UUID skillId, Pageable pageable) {
        Page<Event> page = eventRepository.findAll(
                EventSpecification.search(
                        EventStatus.PUBLISHED, LocalDateTime.now(),
                        type != null ? EventType.valueOf(type) : null,
                        city, orgId, online, startAfter, startBefore, skillId),
                pageable);
        return page.map(this::toSummary);
    }

    // T-074: Get event detail
    @Transactional(readOnly = true)
    public EventDetail getEvent(UUID eventId) {
        Event event = findEvent(eventId);
        return toDetail(event);
    }

    // T-075: Signup for event
    @Transactional
    public SignupResponse signup(UUID userId, UUID eventId) {
        Event event = findEvent(eventId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("L'inscription n'est possible que pour les événements publiés");
        }

        // Check deadline
        LocalDateTime deadline = event.getRegistrationDeadline() != null
                ? event.getRegistrationDeadline() : event.getStartDate();
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new BusinessRuleException("La date limite d'inscription est dépassée");
        }

        // Check duplicate
        if (eventSignupRepository.existsByEventIdAndUserIdAndStatusIn(eventId, userId,
                List.of(SignupStatus.REGISTERED, SignupStatus.WAITLISTED))) {
            throw new ConflictException("Vous êtes déjà inscrit à cet événement");
        }

        // Check min age
        if (event.getMinAge() != null && user.getDateOfBirth() != null) {
            int age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
            if (age < event.getMinAge()) {
                throw new BusinessRuleException("Vous devez avoir au moins " + event.getMinAge() + " ans pour vous inscrire");
            }
        }

        // Determine status
        long registeredCount = eventSignupRepository.countByEventIdAndStatus(eventId, SignupStatus.REGISTERED);
        SignupStatus status;
        if (event.getMaxParticipants() == null || registeredCount < event.getMaxParticipants()) {
            status = SignupStatus.REGISTERED;
        } else {
            status = SignupStatus.WAITLISTED;
        }

        EventSignup signup = new EventSignup();
        signup.setEvent(event);
        signup.setUser(user);
        signup.setStatus(status);
        signup.setRegisteredAt(LocalDateTime.now());
        signup = eventSignupRepository.save(signup);

        NotificationType notifType = status == SignupStatus.REGISTERED
                ? NotificationType.SIGNUP_CONFIRMED : NotificationType.SIGNUP_WAITLISTED;
        String msg = status == SignupStatus.REGISTERED
                ? "Vous êtes inscrit à '" + event.getTitle() + "'"
                : "Vous êtes sur la liste d'attente pour '" + event.getTitle() + "'";
        createNotification(user, notifType, "Inscription événement", msg, "/events/" + eventId);

        audit(userId, "EVENT_SIGNUP", "EventSignup", signup.getId());
        return toSignupResponse(signup);
    }

    // T-076: Cancel signup
    @Transactional
    public void cancelSignup(UUID userId, UUID eventId) {
        EventSignup signup = eventSignupRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription non trouvée"));

        if (signup.getStatus() == SignupStatus.CANCELLED || signup.getStatus() == SignupStatus.ATTENDED) {
            throw new BusinessRuleException("Impossible d'annuler cette inscription");
        }

        boolean wasRegistered = signup.getStatus() == SignupStatus.REGISTERED;
        signup.setStatus(SignupStatus.CANCELLED);
        signup.setCancelledAt(LocalDateTime.now());
        eventSignupRepository.save(signup);

        // FIFO promotion from waitlist
        if (wasRegistered) {
            Event event = signup.getEvent();
            if (event.getMaxParticipants() != null) {
                eventSignupRepository.findFirstByEventIdAndStatusOrderByRegisteredAtAsc(
                        eventId, SignupStatus.WAITLISTED).ifPresent(waitlisted -> {
                    waitlisted.setStatus(SignupStatus.REGISTERED);
                    eventSignupRepository.save(waitlisted);
                    createNotification(waitlisted.getUser(), NotificationType.SIGNUP_PROMOTED,
                            "Place libérée !", "Une place s'est libérée pour '" + event.getTitle() + "'. Vous êtes désormais inscrit !",
                            "/events/" + eventId);
                });
            }
        }

        audit(userId, "EVENT_SIGNUP_CANCELLED", "EventSignup", signup.getId());
    }

    // T-077: List signups
    @Transactional(readOnly = true)
    public Page<SignupResponse> listSignups(UUID userId, UUID eventId, Pageable pageable) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());
        return eventSignupRepository.findByEventId(eventId, pageable).map(this::toSignupResponse);
    }

    // T-078: Export signups CSV
    @Transactional(readOnly = true)
    public String exportSignupsCsv(UUID userId, UUID eventId) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());

        List<EventSignup> signups = eventSignupRepository.findByEventId(eventId, Pageable.unpaged()).getContent();
        StringBuilder csv = new StringBuilder("Prénom,Nom,Email,Statut,Date inscription\n");
        for (EventSignup s : signups) {
            csv.append(String.format("%s,%s,%s,%s,%s\n",
                    s.getUser().getFirstName(), s.getUser().getLastName(),
                    s.getUser().getEmail(), s.getStatus().name(), s.getRegisteredAt()));
        }
        return csv.toString();
    }

    // T-079: Mark attendance
    @Transactional
    public void markAttendance(UUID userId, UUID eventId, AttendanceRequest request) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());

        for (UUID attendeeId : request.userIds()) {
            eventSignupRepository.findByEventIdAndUserId(eventId, attendeeId).ifPresent(signup -> {
                if (signup.getStatus() == SignupStatus.REGISTERED) {
                    signup.setStatus(SignupStatus.ATTENDED);
                    signup.setAttendedAt(LocalDateTime.now());
                    eventSignupRepository.save(signup);
                    createNotification(signup.getUser(), NotificationType.FEEDBACK_REQUESTED,
                            "Donnez votre avis", "Comment s'est passé '" + event.getTitle() + "' ? Laissez un feedback !",
                            "/events/" + eventId);
                }
            });
        }

        audit(userId, "EVENT_ATTENDANCE_MARKED", "Event", eventId);
    }

    // T-080: Create feedback
    @Transactional
    public FeedbackResponse createFeedback(UUID userId, UUID eventId, CreateFeedbackRequest request) {
        Event event = findEvent(eventId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Verify ATTENDED
        EventSignup signup = eventSignupRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BusinessRuleException("Vous n'êtes pas inscrit à cet événement"));
        if (signup.getStatus() != SignupStatus.ATTENDED) {
            throw new BusinessRuleException("Seuls les participants ayant assisté à l'événement peuvent laisser un feedback");
        }

        // Uniqueness
        if (eventFeedbackRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new ConflictException("Vous avez déjà laissé un feedback pour cet événement");
        }

        EventFeedback feedback = new EventFeedback();
        feedback.setEvent(event);
        feedback.setUser(user);
        feedback.setRating(request.rating());
        feedback.setComment(request.comment());
        feedback.setAnonymous(request.anonymous());
        feedback = eventFeedbackRepository.save(feedback);

        return toFeedbackResponse(feedback);
    }

    // T-081: List feedbacks
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> listFeedbacks(UUID eventId, Pageable pageable) {
        return eventFeedbackRepository.findByEventId(eventId, pageable).map(this::toFeedbackResponse);
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(UUID eventId) {
        return eventFeedbackRepository.findAverageRatingByEventId(eventId);
    }

    // T-111: List user signups
    @Transactional(readOnly = true)
    public Page<SignupResponse> listUserSignups(UUID userId, Pageable pageable) {
        return eventSignupRepository.findByUserId(userId, pageable).map(this::toSignupResponse);
    }

    // --- Helpers ---

    Event findEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé"));
    }

    private void validateDates(LocalDateTime startDate, LocalDateTime endDate, LocalDateTime deadline) {
        if (startDate.isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("La date de début doit être dans le futur");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("La date de fin doit être après la date de début");
        }
        if (deadline != null && deadline.isAfter(startDate)) {
            throw new BusinessRuleException("La date limite d'inscription doit être avant la date de début");
        }
    }

    EventDetail toDetail(Event event) {
        long registeredCount = eventSignupRepository.countByEventIdAndStatus(event.getId(), SignupStatus.REGISTERED);
        long waitlistedCount = eventSignupRepository.countByEventIdAndStatus(event.getId(), SignupStatus.WAITLISTED);
        Integer availableSpots = event.getMaxParticipants() != null
                ? Math.max(0, event.getMaxParticipants() - (int) registeredCount) : null;

        List<EventDetail.SkillDto> skills = event.getRequiredSkills().stream()
                .map(s -> new EventDetail.SkillDto(s.getId(), s.getName(), s.getCategory().name()))
                .toList();

        Double avgRating = eventFeedbackRepository.findAverageRatingByEventId(event.getId());

        Organization org = event.getOrganization();
        return new EventDetail(
                event.getId(), event.getTitle(), event.getDescription(), event.getObjectives(),
                event.getType().name(), event.getLocationName(), event.getLocationAddress(),
                event.getLocationCity(), event.getLocationZip(), event.getLocationLat(), event.getLocationLng(),
                event.isOnline(), event.getOnlineLink(),
                event.getStartDate(), event.getEndDate(), event.getRegistrationDeadline(),
                event.getMaxParticipants(), event.getMinAge(),
                event.getStatus().name(), event.getCancellationReason(),
                org.getId(), org.getName(), org.getSlug(),
                registeredCount, waitlistedCount, availableSpots, skills, avgRating,
                event.getCreatedAt());
    }

    EventSummary toSummaryPublic(Event event) {
        return toSummary(event);
    }

    private EventSummary toSummary(Event event) {
        long registeredCount = eventSignupRepository.countByEventIdAndStatus(event.getId(), SignupStatus.REGISTERED);
        Organization org = event.getOrganization();
        return new EventSummary(event.getId(), event.getTitle(), event.getType().name(),
                event.getLocationCity(), event.isOnline(), event.getStartDate(), event.getEndDate(),
                event.getMaxParticipants(), registeredCount, event.getStatus().name(),
                org.getName(), org.getSlug());
    }

    SignupResponse toSignupResponse(EventSignup signup) {
        User user = signup.getUser();
        return new SignupResponse(signup.getId(), signup.getEvent().getId(), user.getId(),
                user.getFirstName(), user.getLastName(), user.getEmail(),
                signup.getStatus().name(), signup.getRegisteredAt(), signup.getAttendedAt());
    }

    private FeedbackResponse toFeedbackResponse(EventFeedback feedback) {
        return new FeedbackResponse(feedback.getId(), feedback.getEvent().getId(),
                feedback.getRating(), feedback.getComment(), feedback.isAnonymous(),
                feedback.isAnonymous() ? null : feedback.getUser().getFirstName(),
                feedback.isAnonymous() ? null : feedback.getUser().getLastName(),
                feedback.getCreatedAt());
    }

    private void createNotification(User user, NotificationType type, String title, String message, String link) {
        Notification notif = new Notification();
        notif.setUser(user);
        notif.setType(type);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setLink(link);
        notificationRepository.save(notif);
    }

    private void audit(UUID actorId, String action, String entityType, UUID entityId) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        auditLogRepository.save(log);
    }
}

