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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;

@Service
public class EventService {

    /** Garde-fou : nombre maximum de créneaux générés pour une série (récurrence + explicites). */
    static final int MAX_OCCURRENCES = 100;

    private final EventRepository eventRepository;
    private final EventOccurrenceRepository occurrenceRepository;
    private final EventSignupRepository eventSignupRepository;
    private final EventFeedbackRepository eventFeedbackRepository;
    private final OrganizationService organizationService;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final MembershipRepository membershipRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final EventPhotoRepository eventPhotoRepository;
    private final ImageService imageService;

    public EventService(EventRepository eventRepository, EventOccurrenceRepository occurrenceRepository,
                        EventSignupRepository eventSignupRepository,
                        EventFeedbackRepository eventFeedbackRepository, OrganizationService organizationService,
                        UserRepository userRepository, SkillRepository skillRepository,
                        MembershipRepository membershipRepository, NotificationService notificationService,
                        AuditService auditService, EventPhotoRepository eventPhotoRepository,
                        ImageService imageService) {
        this.eventRepository = eventRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.eventSignupRepository = eventSignupRepository;
        this.eventFeedbackRepository = eventFeedbackRepository;
        this.organizationService = organizationService;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.membershipRepository = membershipRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.eventPhotoRepository = eventPhotoRepository;
        this.imageService = imageService;
    }

    /** Garde-fou galerie : au-delà, c'est un album photo, plus une preuve d'impact. */
    static final int MAX_PHOTOS_PER_EVENT = 30;

    // T-070: Create event (+ ses créneaux)
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

        // Créneaux : créneau principal + récurrence + créneaux explicites additionnels.
        List<EventOccurrence> occurrences = buildOccurrences(event, request);
        occurrenceRepository.saveAll(occurrences);

        // L'événement (série) porte une enveloppe de dates = min début / max fin des créneaux.
        event.setStartDate(occurrences.stream().map(EventOccurrence::getStartDate)
                .min(Comparator.naturalOrder()).orElse(request.startDate()));
        event.setEndDate(occurrences.stream().map(EventOccurrence::getEndDate)
                .max(Comparator.naturalOrder()).orElse(request.endDate()));
        event = eventRepository.save(event);

        auditService.log(userId, "EVENT_CREATED", "Event", event.getId());
        return toDetail(event, userId);
    }

    // T-071: Update event (métadonnées de la série ; dates/capacité limitées, cf. créneaux)
    @Transactional
    public EventDetail updateEvent(UUID userId, UUID eventId, UpdateEventRequest request) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());

        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) {
            throw new BusinessRuleException("Impossible de modifier un événement " + event.getStatus().name().toLowerCase());
        }

        boolean touchesSchedule = request.startDate() != null || request.endDate() != null
                || request.maxParticipants() != null || request.registrationDeadline() != null;
        if (touchesSchedule && event.getStatus() == EventStatus.PUBLISHED) {
            throw new BusinessRuleException("Impossible de modifier les dates ou le nombre max de participants d'un événement publié");
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
        if (request.minAge() != null) event.setMinAge(request.minAge());
        if (request.requiredSkillIds() != null) {
            Set<Skill> skills = new HashSet<>(skillRepository.findAllById(request.requiredSkillIds()));
            event.setRequiredSkills(skills);
        }

        // Modification d'horaire/capacité : uniquement pour un événement mono-créneau en brouillon.
        // Les séries multi-créneaux se modifient via les endpoints de créneaux.
        if (touchesSchedule) {
            List<EventOccurrence> occs = occurrenceRepository.findByEventIdOrderByStartDateAsc(eventId);
            if (occs.size() != 1) {
                throw new BusinessRuleException("Cet événement a plusieurs créneaux : modifiez-les via les créneaux.");
            }
            EventOccurrence occ = occs.get(0);
            LocalDateTime newStart = request.startDate() != null ? request.startDate() : occ.getStartDate();
            LocalDateTime newEnd = request.endDate() != null ? request.endDate() : occ.getEndDate();
            LocalDateTime newDeadline = request.registrationDeadline() != null ? request.registrationDeadline() : occ.getRegistrationDeadline();
            validateDates(newStart, newEnd, newDeadline);
            occ.setStartDate(newStart);
            occ.setEndDate(newEnd);
            occ.setRegistrationDeadline(newDeadline);
            if (request.maxParticipants() != null) occ.setMaxParticipants(request.maxParticipants());
            occurrenceRepository.save(occ);
            event.setStartDate(newStart);
            event.setEndDate(newEnd);
            event.setRegistrationDeadline(newDeadline);
            if (request.maxParticipants() != null) event.setMaxParticipants(request.maxParticipants());
        }

        event = eventRepository.save(event);
        auditService.log(userId, "EVENT_UPDATED", "Event", eventId);
        return toDetail(event, userId);
    }

    // T-072: Change series status
    @Transactional
    public EventDetail changeStatus(UUID userId, UUID eventId, EventStatusRequest request) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());

        String action = request.status().toUpperCase();
        List<EventOccurrence> occurrences = occurrenceRepository.findByEventIdOrderByStartDateAsc(eventId);

        switch (action) {
            case "PUBLISH" -> {
                if (event.getStatus() != EventStatus.DRAFT) {
                    throw new BusinessRuleException("Seul un événement en brouillon peut être publié");
                }
                event.setStatus(EventStatus.PUBLISHED);
                for (EventOccurrence o : occurrences) {
                    if (o.getStatus() == EventOccurrenceStatus.DRAFT) {
                        o.setStatus(EventOccurrenceStatus.PUBLISHED);
                    }
                }
                occurrenceRepository.saveAll(occurrences);
                String publishTitle = event.getTitle();
                String publishOrgName = event.getOrganization().getName();
                membershipRepository.findByOrganizationIdAndRoleInAndStatus(
                        event.getOrganization().getId(),
                        List.of(MembershipRole.MEMBER, MembershipRole.ADMIN),
                        MembershipStatus.ACTIVE)
                        .forEach(m -> notificationService.saveNotification(m.getUser(), NotificationType.EVENT_PUBLISHED,
                                "Nouvel événement", "'" + publishTitle + "' par " + publishOrgName,
                                "/events/" + eventId));
                auditService.log(userId, "EVENT_PUBLISHED", "Event", eventId);
            }
            case "CANCEL" -> {
                if (request.reason() == null || request.reason().length() < 10) {
                    throw new BusinessRuleException("Le motif d'annulation doit faire au moins 10 caractères");
                }
                // Un événement déjà annulé ou terminé ne se ré-annule pas : sans ce garde-fou,
                // une mission COMPLETED repassait en CANCELLED, effaçant l'historique de
                // participation et renotifiant des bénévoles pour un événement déjà passé.
                if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) {
                    throw new BusinessRuleException(
                            "Un événement " + (event.getStatus() == EventStatus.CANCELLED ? "annulé" : "terminé")
                                    + " ne peut plus être annulé");
                }
                event.setStatus(EventStatus.CANCELLED);
                event.setCancellationReason(request.reason());
                for (EventOccurrence o : occurrences) {
                    o.setStatus(EventOccurrenceStatus.CANCELLED);
                    o.setCancellationReason(request.reason());
                }
                occurrenceRepository.saveAll(occurrences);
                // Annuler toutes les inscriptions actives de la série + notifier.
                List<EventSignup> signups = eventSignupRepository.findByEventIdAndStatusIn(
                        eventId, List.of(SignupStatus.REGISTERED, SignupStatus.WAITLISTED));
                for (EventSignup s : signups) {
                    s.setStatus(SignupStatus.CANCELLED);
                    s.setCancelledAt(LocalDateTime.now());
                    notificationService.saveNotification(s.getUser(), NotificationType.EVENT_CANCELLED,
                            "Événement annulé", "'" + event.getTitle() + "' a été annulé. Motif : " + request.reason(),
                            "/events/" + event.getId());
                }
                eventSignupRepository.saveAll(signups);
                auditService.log(userId, "EVENT_CANCELLED", "Event", eventId);
            }
            case "COMPLETE" -> {
                if (event.getStatus() != EventStatus.PUBLISHED) {
                    throw new BusinessRuleException("Seul un événement publié peut être marqué comme terminé");
                }
                event.setStatus(EventStatus.COMPLETED);
                for (EventOccurrence o : occurrences) {
                    if (o.getStatus() == EventOccurrenceStatus.PUBLISHED) {
                        o.setStatus(EventOccurrenceStatus.COMPLETED);
                    }
                }
                occurrenceRepository.saveAll(occurrences);
                auditService.log(userId, "EVENT_COMPLETED", "Event", eventId);
            }
            default -> throw new BusinessRuleException("Action invalide. Utilisez PUBLISH, CANCEL ou COMPLETE.");
        }

        event = eventRepository.save(event);
        return toDetail(event, userId);
    }

    // Annuler / compléter un seul créneau d'une série
    @Transactional
    public EventDetail changeOccurrenceStatus(UUID userId, UUID eventId, UUID occurrenceId, EventStatusRequest request) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());
        EventOccurrence occ = requireOccurrenceInEvent(occurrenceId, eventId);

        String action = request.status().toUpperCase();
        switch (action) {
            case "CANCEL" -> {
                if (request.reason() == null || request.reason().length() < 10) {
                    throw new BusinessRuleException("Le motif d'annulation doit faire au moins 10 caractères");
                }
                if (occ.getStatus() == EventOccurrenceStatus.CANCELLED || occ.getStatus() == EventOccurrenceStatus.COMPLETED) {
                    throw new BusinessRuleException("Ce créneau ne peut plus être annulé");
                }
                occ.setStatus(EventOccurrenceStatus.CANCELLED);
                occ.setCancellationReason(request.reason());
                occurrenceRepository.save(occ);
                // Annuler + notifier uniquement les inscrits de CE créneau.
                List<EventSignup> signups = eventSignupRepository.findByOccurrenceIdAndStatusIn(
                        occurrenceId, List.of(SignupStatus.REGISTERED, SignupStatus.WAITLISTED));
                for (EventSignup s : signups) {
                    s.setStatus(SignupStatus.CANCELLED);
                    s.setCancelledAt(LocalDateTime.now());
                    notificationService.saveNotification(s.getUser(), NotificationType.EVENT_CANCELLED,
                            "Créneau annulé", "Un créneau de '" + event.getTitle() + "' a été annulé. Motif : " + request.reason(),
                            "/events/" + eventId);
                }
                eventSignupRepository.saveAll(signups);
                auditService.log(userId, "EVENT_OCCURRENCE_CANCELLED", "EventOccurrence", occurrenceId);
            }
            case "COMPLETE" -> {
                if (occ.getStatus() != EventOccurrenceStatus.PUBLISHED) {
                    throw new BusinessRuleException("Seul un créneau publié peut être marqué comme terminé");
                }
                occ.setStatus(EventOccurrenceStatus.COMPLETED);
                occurrenceRepository.save(occ);
                auditService.log(userId, "EVENT_OCCURRENCE_COMPLETED", "EventOccurrence", occurrenceId);
            }
            default -> throw new BusinessRuleException("Action invalide pour un créneau. Utilisez CANCEL ou COMPLETE.");
        }
        return toDetail(event, userId);
    }

    // T-073: List events (public)
    @Transactional(readOnly = true)
    public Page<EventSummary> listEvents(EventSearchCriteria criteria, Pageable pageable) {
        Page<Event> page = eventRepository.findAll(
                EventSpecification.search(EventStatus.PUBLISHED, LocalDateTime.now(), criteria),
                pageable);
        return page.map(event -> toSummary(event, criteria));
    }

    // List all events of an org (all statuses) — org admin only
    @Transactional(readOnly = true)
    public Page<EventSummary> listOrgEvents(UUID userId, UUID orgId, Pageable pageable) {
        organizationService.verifyAdmin(userId, orgId);
        return eventRepository.findByOrganizationId(orgId, pageable).map(this::toSummary);
    }

    // T-074: Get event detail
    @Transactional(readOnly = true)
    public EventDetail getEvent(UUID eventId, UUID userId) {
        Event event = findEvent(eventId);
        return toDetail(event, userId);
    }

    // T-075: Signup — événement mono-créneau (rétro-compatible)
    @Transactional
    public SignupResponse signup(UUID userId, UUID eventId) {
        Event event = findEvent(eventId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        EventOccurrence occ = resolveSoleOccurrence(event);
        return doSignup(user, event, occ);
    }

    // T-075bis: Signup à un créneau précis
    @Transactional
    public SignupResponse signupToOccurrence(UUID userId, UUID eventId, UUID occurrenceId) {
        Event event = findEvent(eventId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        EventOccurrence occ = requireOccurrenceInEvent(occurrenceId, eventId);
        return doSignup(user, event, occ);
    }

    private SignupResponse doSignup(User user, Event event, EventOccurrence requestedOccurrence) {
        // Verrou sur la ligne du créneau pour toute la transaction : le comptage des
        // places et l'écriture de l'inscription qui suivent doivent être atomiques,
        // sinon deux inscriptions simultanées lisent la même place restante et
        // l'événement part en surnombre.
        final EventOccurrence occ = occurrenceRepository.findByIdForUpdate(requestedOccurrence.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Créneau non trouvé"));

        if (occ.getStatus() != EventOccurrenceStatus.PUBLISHED) {
            throw new BusinessRuleException("L'inscription n'est possible que pour les créneaux publiés");
        }
        if (LocalDateTime.now().isAfter(occ.effectiveDeadline())) {
            throw new BusinessRuleException("La date limite d'inscription est dépassée");
        }

        Optional<EventSignup> existingOpt = eventSignupRepository.findByOccurrenceIdAndUserId(occ.getId(), user.getId());
        if (existingOpt.isPresent()) {
            SignupStatus existingStatus = existingOpt.get().getStatus();
            if (existingStatus == SignupStatus.REGISTERED || existingStatus == SignupStatus.WAITLISTED) {
                throw new ConflictException("Vous êtes déjà inscrit à ce créneau");
            }
            if (existingStatus == SignupStatus.ATTENDED) {
                throw new BusinessRuleException("Vous avez déjà participé à ce créneau");
            }
        }

        // Âge minimum : l'absence de date de naissance ne vaut pas autorisation. Le
        // contrôle était auparavant sauté dans ce cas, ce qui laissait n'importe qui
        // s'inscrire à une mission réservée aux majeurs en ne renseignant pas son profil.
        if (event.getMinAge() != null) {
            if (user.getDateOfBirth() == null) {
                throw new BusinessRuleException(
                        "Cette mission est réservée aux " + event.getMinAge() + " ans et plus. "
                                + "Renseignez votre date de naissance dans votre profil pour vous inscrire.");
            }
            int age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
            if (age < event.getMinAge()) {
                throw new BusinessRuleException("Vous devez avoir au moins " + event.getMinAge() + " ans pour vous inscrire");
            }
        }

        long registeredCount = eventSignupRepository.countByOccurrenceIdAndStatus(occ.getId(), SignupStatus.REGISTERED);
        SignupStatus status = (occ.getMaxParticipants() == null || registeredCount < occ.getMaxParticipants())
                ? SignupStatus.REGISTERED : SignupStatus.WAITLISTED;

        EventSignup signup = existingOpt.orElseGet(() -> {
            EventSignup s = new EventSignup();
            s.setEvent(event);
            s.setOccurrence(occ);
            s.setUser(user);
            return s;
        });
        signup.setStatus(status);
        signup.setRegisteredAt(LocalDateTime.now());
        signup.setCancelledAt(null);
        signup = eventSignupRepository.save(signup);

        NotificationType notifType = status == SignupStatus.REGISTERED
                ? NotificationType.SIGNUP_CONFIRMED : NotificationType.SIGNUP_WAITLISTED;
        String msg = status == SignupStatus.REGISTERED
                ? "Vous êtes inscrit à '" + event.getTitle() + "'"
                : "Vous êtes sur la liste d'attente pour '" + event.getTitle() + "'";
        notificationService.saveNotification(user, notifType, "Inscription événement", msg, "/events/" + event.getId());

        auditService.log(user.getId(), "EVENT_SIGNUP", "EventSignup", signup.getId());
        return toSignupResponse(signup);
    }

    /**
     * Inscription du bénévole pour cet événement, tous créneaux confondus.
     *
     * <p>Une série peut porter plusieurs inscriptions pour le même bénévole (une par
     * créneau) : renvoyer la première venue faisait remonter une inscription annulée
     * alors qu'une inscription active existait, et le front affichait « S'inscrire » à
     * quelqu'un de déjà inscrit. On privilégie donc l'inscription la plus engageante,
     * puis la plus récente.
     */
    @Transactional(readOnly = true)
    public Optional<SignupResponse> getMySignup(UUID userId, UUID eventId) {
        return eventSignupRepository.findByEventIdAndUserId(eventId, userId).stream()
                .min(Comparator
                        .comparingInt((EventSignup s) -> signupRelevance(s.getStatus()))
                        .thenComparing(EventSignup::getRegisteredAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toSignupResponse);
    }

    /** Plus la valeur est basse, plus l'inscription prime dans l'affichage. */
    private static int signupRelevance(SignupStatus status) {
        return switch (status) {
            case REGISTERED -> 0;
            case WAITLISTED -> 1;
            case ATTENDED -> 2;
            case CANCELLED -> 3;
        };
    }

    // T-076: Cancel signup — événement mono-créneau (rétro-compatible)
    @Transactional
    public void cancelSignup(UUID userId, UUID eventId) {
        Event event = findEvent(eventId);
        EventOccurrence occ = resolveSoleOccurrence(event);
        doCancel(userId, event, occ);
    }

    // T-076bis: Cancel signup sur un créneau précis
    @Transactional
    public void cancelOccurrenceSignup(UUID userId, UUID eventId, UUID occurrenceId) {
        Event event = findEvent(eventId);
        EventOccurrence occ = requireOccurrenceInEvent(occurrenceId, eventId);
        doCancel(userId, event, occ);
    }

    private void doCancel(UUID userId, Event event, EventOccurrence occ) {
        EventSignup signup = eventSignupRepository.findByOccurrenceIdAndUserId(occ.getId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription non trouvée"));

        if (signup.getStatus() == SignupStatus.CANCELLED || signup.getStatus() == SignupStatus.ATTENDED
                || signup.getStatus() == SignupStatus.NO_SHOW) {
            throw new BusinessRuleException("Impossible d'annuler cette inscription");
        }

        boolean wasRegistered = signup.getStatus() == SignupStatus.REGISTERED;
        // Annulation tardive : moins de 24h avant le début du créneau (impacte la fiabilité).
        if (wasRegistered && occ.getStartDate() != null
                && LocalDateTime.now().isAfter(occ.getStartDate().minusHours(24))) {
            signup.setLateCancel(true);
        }
        signup.setStatus(SignupStatus.CANCELLED);
        signup.setCancelledAt(LocalDateTime.now());
        eventSignupRepository.save(signup);

        // Promotion FIFO de la liste d'attente — isolée au créneau.
        if (wasRegistered && occ.getMaxParticipants() != null) {
            eventSignupRepository.findFirstByOccurrenceIdAndStatusOrderByRegisteredAtAsc(
                    occ.getId(), SignupStatus.WAITLISTED).ifPresent(waitlisted -> {
                waitlisted.setStatus(SignupStatus.REGISTERED);
                eventSignupRepository.save(waitlisted);
                notificationService.saveNotification(waitlisted.getUser(), NotificationType.SIGNUP_PROMOTED,
                        "Place libérée !", "Une place s'est libérée pour '" + event.getTitle() + "'. Vous êtes désormais inscrit !",
                        "/events/" + event.getId());
            });
        }

        auditService.log(userId, "EVENT_SIGNUP_CANCELLED", "EventSignup", signup.getId());
    }

    // T-077: List signups de la série (admin ONG)
    @Transactional(readOnly = true)
    public Page<SignupResponse> listSignups(UUID userId, UUID eventId, Pageable pageable) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());
        return eventSignupRepository.findByEventId(eventId, pageable).map(this::toSignupResponse);
    }

    // List signups d'un créneau précis (admin ONG)
    @Transactional(readOnly = true)
    public Page<SignupResponse> listOccurrenceSignups(UUID userId, UUID eventId, UUID occurrenceId, Pageable pageable) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());
        requireOccurrenceInEvent(occurrenceId, eventId);
        return eventSignupRepository.findByOccurrenceId(occurrenceId, pageable).map(this::toSignupResponse);
    }

    // T-078: Export signups CSV (admin ONG)
    @Transactional(readOnly = true)
    public String exportSignupsCsv(UUID userId, UUID eventId) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());

        List<EventSignup> signups = eventSignupRepository.findByEventId(eventId, Pageable.unpaged()).getContent();
        StringBuilder csv = new StringBuilder("Prénom,Nom,Email,Créneau,Statut,Date inscription\n");
        for (EventSignup s : signups) {
            csv.append(String.join(",",
                    csvCell(s.getUser().getFirstName()),
                    csvCell(s.getUser().getLastName()),
                    csvCell(s.getUser().getEmail()),
                    csvCell(s.getOccurrence() != null ? s.getOccurrence().getStartDate().toString() : ""),
                    csvCell(s.getStatus().name()),
                    csvCell(String.valueOf(s.getRegisteredAt()))));
            csv.append("\n");
        }
        return csv.toString();
    }

    /**
     * Encode une valeur en cellule CSV.
     *
     * <p>Deux problèmes traités :
     * <ul>
     *   <li>un nom contenant une virgule, un guillemet ou un saut de ligne décalait toutes
     *       les colonnes suivantes — la valeur est donc systématiquement entre guillemets,
     *       les guillemets internes étant doublés (RFC 4180) ;</li>
     *   <li>un nom commençant par {@code = + - @} (ou une tabulation) est interprété comme
     *       une formule par Excel et LibreOffice à l'ouverture du fichier. Comme ces champs
     *       viennent des utilisateurs, on préfixe d'une apostrophe pour neutraliser
     *       l'exécution.</li>
     * </ul>
     */
    static String csvCell(String value) {
        String safe = value == null ? "" : value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return '"' + safe.replace("\"", "\"\"") + '"';
    }

    // T-079: Mark attendance — événement mono-créneau (rétro-compatible)
    @Transactional
    public void markAttendance(UUID userId, UUID eventId, AttendanceRequest request) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());
        EventOccurrence occ = resolveSoleOccurrence(event);
        doMarkAttendance(userId, event, occ, request);
    }

    // T-079bis: Mark attendance sur un créneau précis
    @Transactional
    public void markOccurrenceAttendance(UUID userId, UUID eventId, UUID occurrenceId, AttendanceRequest request) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());
        EventOccurrence occ = requireOccurrenceInEvent(occurrenceId, eventId);
        doMarkAttendance(userId, event, occ, request);
    }

    private void doMarkAttendance(UUID actorUserId, Event event, EventOccurrence occ, AttendanceRequest request) {
        for (UUID attendeeId : request.userIds()) {
            eventSignupRepository.findByOccurrenceIdAndUserId(occ.getId(), attendeeId).ifPresent(signup -> {
                // REGISTERED → ATTENDED ; NO_SHOW → ATTENDED permet à l'ONG de corriger une absence.
                if (signup.getStatus() != SignupStatus.REGISTERED && signup.getStatus() != SignupStatus.NO_SHOW) {
                    return;
                }
                // L'avis se donne une fois par événement, pas une fois par créneau : sur une
                // série récurrente, notifier à chaque présence validée envoyait dix
                // « donnez votre avis » pour un seul avis possible.
                boolean firstAttendance = !eventSignupRepository.existsByEventIdAndUserIdAndStatus(
                        event.getId(), attendeeId, SignupStatus.ATTENDED);

                signup.setStatus(SignupStatus.ATTENDED);
                signup.setAttendedAt(LocalDateTime.now());
                // Heures pré-remplies avec la durée du créneau (l'ONG pourra ajuster ensuite).
                signup.setHoursValidated(occurrenceDurationHours(occ));
                eventSignupRepository.save(signup);

                if (firstAttendance) {
                    notificationService.saveNotification(signup.getUser(), NotificationType.FEEDBACK_REQUESTED,
                            "Donnez votre avis", "Comment s'est passé '" + event.getTitle() + "' ? Laissez un feedback !",
                            "/events/" + event.getId());
                }
            });
        }
        auditService.log(actorUserId, "EVENT_ATTENDANCE_MARKED", "EventOccurrence", occ.getId());
    }

    // Ajustement des heures certifiées d'une présence par l'ONG.
    @Transactional
    public SignupResponse adjustSignupHours(UUID adminUserId, UUID eventId, UUID occurrenceId,
                                            UUID signupId, BigDecimal hours) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(adminUserId, event.getOrganization().getId());
        requireOccurrenceInEvent(occurrenceId, eventId);

        EventSignup signup = eventSignupRepository.findById(signupId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription non trouvée"));
        if (signup.getOccurrence() == null || !signup.getOccurrence().getId().equals(occurrenceId)) {
            throw new BusinessRuleException("Cette inscription n'appartient pas à ce créneau");
        }
        if (signup.getStatus() != SignupStatus.ATTENDED) {
            throw new BusinessRuleException("Seule une présence validée peut recevoir des heures");
        }
        if (hours == null || hours.signum() < 0 || hours.compareTo(new BigDecimal("999.99")) > 0) {
            throw new BusinessRuleException("Nombre d'heures invalide (0 à 999,99)");
        }
        signup.setHoursValidated(hours.setScale(2, RoundingMode.HALF_UP));
        eventSignupRepository.save(signup);
        auditService.log(adminUserId, "EVENT_HOURS_ADJUSTED", "EventSignup", signupId);
        return toSignupResponse(signup);
    }

    // Marquage d'absence par l'ONG (jamais automatique) : REGISTERED → NO_SHOW.
    @Transactional
    public void markNoShow(UUID adminUserId, UUID eventId, UUID occurrenceId, AttendanceRequest request) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(adminUserId, event.getOrganization().getId());
        EventOccurrence occ = requireOccurrenceInEvent(occurrenceId, eventId);
        for (UUID attendeeId : request.userIds()) {
            eventSignupRepository.findByOccurrenceIdAndUserId(occ.getId(), attendeeId).ifPresent(signup -> {
                if (signup.getStatus() == SignupStatus.REGISTERED) {
                    signup.setStatus(SignupStatus.NO_SHOW);
                    eventSignupRepository.save(signup);
                }
            });
        }
        auditService.log(adminUserId, "EVENT_NO_SHOW_MARKED", "EventOccurrence", occurrenceId);
    }

    // T-080: Create feedback (au niveau série : avoir participé à au moins un créneau)
    @Transactional
    public FeedbackResponse createFeedback(UUID userId, UUID eventId, CreateFeedbackRequest request) {
        Event event = findEvent(eventId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!eventSignupRepository.existsByEventIdAndUserIdAndStatus(eventId, userId, SignupStatus.ATTENDED)) {
            throw new BusinessRuleException("Seuls les participants ayant assisté à l'événement peuvent laisser un feedback");
        }

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

    // Fiabilité d'un bénévole — réservée aux admins de l'ONG (donnée de profilage, jamais publique).
    @Transactional(readOnly = true)
    public ReliabilityResponse getReliability(UUID callerId, UUID orgId, UUID volunteerId) {
        organizationService.verifyAdmin(callerId, orgId);
        long attended = eventSignupRepository.countByUserIdAndStatus(volunteerId, SignupStatus.ATTENDED);
        long noShow = eventSignupRepository.countByUserIdAndStatus(volunteerId, SignupStatus.NO_SHOW);
        long lateCancel = eventSignupRepository.countByUserIdAndLateCancelTrue(volunteerId);
        long denom = attended + noShow + lateCancel;
        Double rate = denom == 0 ? null : Math.round((double) attended / denom * 100.0) / 100.0;
        return new ReliabilityResponse(volunteerId, attended, noShow, lateCancel, rate);
    }

    // --- Génération des créneaux ---

    private List<EventOccurrence> buildOccurrences(Event event, CreateEventRequest request) {
        List<EventOccurrence> result = new ArrayList<>();

        // 1) Créneau principal (toujours présent).
        result.add(newOccurrence(event, null, request.startDate(), request.endDate(),
                request.registrationDeadline(), request.maxParticipants()));

        // 2) Récurrence : répète le créneau principal.
        if (request.recurrence() != null) {
            result.addAll(generateRecurrence(event, request));
        }

        // 3) Créneaux explicites additionnels (journée multi-créneaux).
        if (request.occurrences() != null) {
            for (OccurrenceInput in : request.occurrences()) {
                validateDates(in.startDate(), in.endDate(), in.registrationDeadline());
                result.add(newOccurrence(event, in.label(), in.startDate(), in.endDate(),
                        in.registrationDeadline(), in.maxParticipants()));
            }
        }

        if (result.size() > MAX_OCCURRENCES) {
            throw new BusinessRuleException("Trop de créneaux (maximum " + MAX_OCCURRENCES + ")");
        }
        return result;
    }

    private List<EventOccurrence> generateRecurrence(Event event, CreateEventRequest request) {
        RecurrenceInput r = request.recurrence();
        String freq = r.frequency() == null ? "" : r.frequency().toUpperCase();
        if (!freq.equals("WEEKLY") && !freq.equals("MONTHLY")) {
            throw new BusinessRuleException("Fréquence de récurrence invalide (WEEKLY ou MONTHLY)");
        }
        int interval = (r.interval() == null || r.interval() < 1) ? 1 : r.interval();
        LocalDate until = r.until();
        if (until.isBefore(request.startDate().toLocalDate())) {
            throw new BusinessRuleException("La fin de récurrence doit être après le premier créneau");
        }

        List<EventOccurrence> list = new ArrayList<>();
        LocalDateTime start = request.startDate();
        LocalDateTime end = request.endDate();
        while (true) {
            if (freq.equals("WEEKLY")) {
                start = start.plusWeeks(interval);
                end = end.plusWeeks(interval);
            } else {
                start = start.plusMonths(interval);
                end = end.plusMonths(interval);
            }
            if (start.toLocalDate().isAfter(until)) break;
            list.add(newOccurrence(event, null, start, end, null, request.maxParticipants()));
            if (list.size() >= MAX_OCCURRENCES) {
                throw new BusinessRuleException("Trop de créneaux générés (maximum " + MAX_OCCURRENCES + ")");
            }
        }
        return list;
    }

    private EventOccurrence newOccurrence(Event event, String label, LocalDateTime start, LocalDateTime end,
                                          LocalDateTime deadline, Integer maxParticipants) {
        EventOccurrence o = new EventOccurrence();
        o.setEvent(event);
        o.setLabel(label);
        o.setStartDate(start);
        o.setEndDate(end);
        o.setRegistrationDeadline(deadline);
        o.setMaxParticipants(maxParticipants);
        o.setStatus(EventOccurrenceStatus.DRAFT);
        return o;
    }

    // --- Helpers ---

    // --- Visuels (couverture & galerie) ---

    /** Remplace l'image de couverture de la mission. Réservé aux admins de l'ONG porteuse. */
    @Transactional
    public String uploadCover(UUID userId, UUID eventId, MultipartFile file) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());
        event.setCoverUrl(imageService.replace(file, "events/" + eventId, event.getCoverUrl()));
        eventRepository.save(event);
        return event.getCoverUrl();
    }

    @Transactional
    public void deleteCover(UUID userId, UUID eventId) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());
        imageService.deleteByPublicUrl(event.getCoverUrl());
        event.setCoverUrl(null);
        eventRepository.save(event);
    }

    /**
     * Ajoute une photo à la galerie de la mission (après coup, comme preuve d'impact).
     * Réservé aux admins de l'ONG porteuse.
     */
    @Transactional
    public EventPhotoResponse addPhoto(UUID userId, UUID eventId, MultipartFile file, String caption) {
        Event event = findEvent(eventId);
        organizationService.verifyAdmin(userId, event.getOrganization().getId());

        if (eventPhotoRepository.countByEventId(eventId) >= MAX_PHOTOS_PER_EVENT) {
            throw new BusinessRuleException(
                    "La galerie est limitée à " + MAX_PHOTOS_PER_EVENT + " photos par événement");
        }

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        List<EventPhoto> existing = eventPhotoRepository.findByEventIdOrderByPositionAscCreatedAtAsc(eventId);
        int nextPosition = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getPosition() + 1;

        EventPhoto photo = new EventPhoto();
        photo.setEvent(event);
        photo.setUrl(imageService.store(file, "events/" + eventId + "/gallery"));
        photo.setCaption(caption);
        photo.setPosition(nextPosition);
        photo.setUploadedBy(uploader);
        photo = eventPhotoRepository.save(photo);

        auditService.log(userId, "EVENT_PHOTO_ADDED", "Event", eventId);
        return toPhotoResponse(photo);
    }

    @Transactional(readOnly = true)
    public List<EventPhotoResponse> listPhotos(UUID eventId) {
        return eventPhotoRepository.findByEventIdOrderByPositionAscCreatedAtAsc(eventId).stream()
                .map(this::toPhotoResponse)
                .toList();
    }

    @Transactional
    public void deletePhoto(UUID userId, UUID photoId) {
        EventPhoto photo = eventPhotoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo non trouvée"));
        organizationService.verifyAdmin(userId, photo.getEvent().getOrganization().getId());
        imageService.deleteByPublicUrl(photo.getUrl());
        eventPhotoRepository.delete(photo);
        auditService.log(userId, "EVENT_PHOTO_DELETED", "Event", photo.getEvent().getId());
    }

    private EventPhotoResponse toPhotoResponse(EventPhoto photo) {
        return new EventPhotoResponse(photo.getId(), photo.getUrl(), photo.getCaption(),
                photo.getPosition(), photo.getCreatedAt());
    }

    Event findEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé"));
    }

    private EventOccurrence resolveSoleOccurrence(Event event) {
        List<EventOccurrence> occs = occurrenceRepository.findByEventIdOrderByStartDateAsc(event.getId());
        if (occs.size() == 1) return occs.get(0);
        if (occs.isEmpty()) throw new ResourceNotFoundException("Aucun créneau pour cet événement");
        throw new BusinessRuleException("Cet événement a plusieurs créneaux : précisez le créneau souhaité");
    }

    private EventOccurrence requireOccurrenceInEvent(UUID occurrenceId, UUID eventId) {
        EventOccurrence occ = occurrenceRepository.findById(occurrenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau non trouvé"));
        if (!occ.getEvent().getId().equals(eventId)) {
            throw new BusinessRuleException("Ce créneau n'appartient pas à cet événement");
        }
        return occ;
    }

    // Durée d'un créneau en heures (2 décimales) — sert de valeur par défaut aux heures validées.
    private BigDecimal occurrenceDurationHours(EventOccurrence occ) {
        if (occ.getStartDate() == null || occ.getEndDate() == null) return BigDecimal.ZERO;
        long minutes = Duration.between(occ.getStartDate(), occ.getEndDate()).toMinutes();
        if (minutes <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
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

    EventDetail toDetail(Event event, UUID userId) {
        List<EventOccurrence> occs = occurrenceRepository.findByEventIdOrderByStartDateAsc(event.getId());
        List<OccurrenceResponse> occResponses = occs.stream().map(o -> toOccurrenceResponse(o, userId)).toList();

        long registeredCount = eventSignupRepository.countByEventIdAndStatus(event.getId(), SignupStatus.REGISTERED);
        long waitlistedCount = eventSignupRepository.countByEventIdAndStatus(event.getId(), SignupStatus.WAITLISTED);

        Integer availableSpots;
        String currentUserSignupStatus;
        if (occResponses.size() == 1) {
            availableSpots = occResponses.get(0).availableSpots();
            currentUserSignupStatus = occResponses.get(0).currentUserSignupStatus();
        } else if (occResponses.isEmpty()) {
            // Repli (aucun créneau chargé, ex. contexte de test unitaire) : logique historique niveau événement.
            availableSpots = event.getMaxParticipants() != null
                    ? Math.max(0, event.getMaxParticipants() - (int) registeredCount) : null;
            currentUserSignupStatus = userId == null ? null :
                    eventSignupRepository.findByEventIdAndUserId(event.getId(), userId).stream()
                            .findFirst().map(s -> s.getStatus().name()).orElse(null);
        } else {
            // Multi-créneaux : les places sont propres à chaque créneau.
            availableSpots = null;
            currentUserSignupStatus = null;
        }

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
                event.getStatus().name(), event.getCancellationReason(), event.getCoverUrl(),
                org.getId(), org.getName(), org.getSlug(), org.getLogoUrl(),
                registeredCount, waitlistedCount, availableSpots, skills, avgRating,
                event.getCreatedAt(), currentUserSignupStatus, occResponses);
    }

    private OccurrenceResponse toOccurrenceResponse(EventOccurrence occ, UUID userId) {
        long registered = eventSignupRepository.countByOccurrenceIdAndStatus(occ.getId(), SignupStatus.REGISTERED);
        long waitlisted = eventSignupRepository.countByOccurrenceIdAndStatus(occ.getId(), SignupStatus.WAITLISTED);
        Integer available = occ.getMaxParticipants() != null
                ? Math.max(0, occ.getMaxParticipants() - (int) registered) : null;
        String currentStatus = userId == null ? null :
                eventSignupRepository.findByOccurrenceIdAndUserId(occ.getId(), userId)
                        .map(s -> s.getStatus().name()).orElse(null);
        return new OccurrenceResponse(occ.getId(), occ.getLabel(), occ.getStartDate(), occ.getEndDate(),
                occ.getRegistrationDeadline(), occ.getMaxParticipants(),
                registered, waitlisted, available, occ.getStatus().name(), currentStatus);
    }

    EventSummary toSummary(Event event) {
        return toSummary(event, null);
    }

    /**
     * @param origin point de référence de la recherche géolocalisée, ou null : sert
     *               uniquement à renseigner {@code distanceKm} dans la réponse.
     */
    EventSummary toSummary(Event event, EventSearchCriteria origin) {
        long registeredCount = eventSignupRepository.countByEventIdAndStatus(event.getId(), SignupStatus.REGISTERED);
        List<EventOccurrence> occs = occurrenceRepository.findByEventIdOrderByStartDateAsc(event.getId());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextOccurrenceDate = occs.stream()
                .filter(o -> o.getStatus() == EventOccurrenceStatus.PUBLISHED && o.getStartDate().isAfter(now))
                .map(EventOccurrence::getStartDate)
                .findFirst()
                .orElse(null);
        Organization org = event.getOrganization();

        Double distanceKm = null;
        if (origin != null && origin.hasOrigin()
                && event.getLocationLat() != null && event.getLocationLng() != null) {
            distanceKm = EventSpecification.distanceKm(
                    origin.lat(), origin.lng(),
                    event.getLocationLat().doubleValue(), event.getLocationLng().doubleValue());
            distanceKm = Math.round(distanceKm * 10.0) / 10.0;
        }

        return new EventSummary(event.getId(), event.getTitle(), event.getType().name(),
                event.getLocationCity(), event.isOnline(), event.getLocationLat(), event.getLocationLng(),
                event.getStartDate(), event.getEndDate(),
                event.getMaxParticipants(), registeredCount, event.getStatus().name(),
                org.getName(), org.getSlug(), org.getLogoUrl(), event.getCoverUrl(),
                nextOccurrenceDate, occs.size(), distanceKm);
    }

    SignupResponse toSignupResponse(EventSignup signup) {
        User user = signup.getUser();
        EventOccurrence occ = signup.getOccurrence();
        return new SignupResponse(signup.getId(), signup.getEvent().getId(), signup.getEvent().getTitle(),
                occ != null ? occ.getId() : null,
                occ != null ? occ.getStartDate() : null,
                occ != null ? occ.getEndDate() : null,
                user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                signup.getStatus().name(), signup.getRegisteredAt(), signup.getAttendedAt(),
                signup.getHoursValidated());
    }

    private FeedbackResponse toFeedbackResponse(EventFeedback feedback) {
        return new FeedbackResponse(feedback.getId(), feedback.getEvent().getId(),
                feedback.getRating(), feedback.getComment(), feedback.isAnonymous(),
                feedback.isAnonymous() ? null : feedback.getUser().getFirstName(),
                feedback.isAnonymous() ? null : feedback.getUser().getLastName(),
                feedback.getCreatedAt());
    }

}
