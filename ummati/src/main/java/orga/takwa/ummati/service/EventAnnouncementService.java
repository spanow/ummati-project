package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.event.AnnouncementResponse;
import orga.takwa.ummati.dto.event.CreateAnnouncementRequest;
import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.EventAnnouncement;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.EventStatus;
import orga.takwa.ummati.entity.enums.NotificationType;
import orga.takwa.ummati.entity.enums.SignupStatus;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.EventAnnouncementRepository;
import orga.takwa.ummati.repository.EventSignupRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EventAnnouncementService {

    private final EventAnnouncementRepository announcementRepository;
    private final EventSignupRepository eventSignupRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final OrganizationService organizationService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public EventAnnouncementService(EventAnnouncementRepository announcementRepository,
                                    EventSignupRepository eventSignupRepository,
                                    UserRepository userRepository,
                                    EventService eventService,
                                    OrganizationService organizationService,
                                    NotificationService notificationService,
                                    AuditService auditService) {
        this.announcementRepository = announcementRepository;
        this.eventSignupRepository = eventSignupRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.organizationService = organizationService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public AnnouncementResponse create(UUID adminId, UUID eventId, CreateAnnouncementRequest request) {
        Event event = eventService.findEvent(eventId);
        organizationService.verifyAdmin(adminId, event.getOrganization().getId());

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("Les annonces ne peuvent être postées que sur un événement publié");
        }

        User author = userRepository.getReferenceById(adminId);
        EventAnnouncement announcement = new EventAnnouncement();
        announcement.setEvent(event);
        announcement.setAuthor(author);
        announcement.setContent(request.content().trim());
        announcement.setPinned(request.pinned());
        announcement = announcementRepository.save(announcement);

        notifyRegistered(event, announcement);
        auditService.log(adminId, "EVENT_ANNOUNCEMENT_CREATED", "EventAnnouncement", announcement.getId());

        return toResponse(announcement);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> list(UUID eventId) {
        eventService.findEvent(eventId);
        return announcementRepository.findByEventIdOrderByPinnedDescCreatedAtAsc(eventId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public AnnouncementResponse update(UUID adminId, UUID announcementId, CreateAnnouncementRequest request) {
        EventAnnouncement announcement = findAnnouncement(announcementId);
        organizationService.verifyAdmin(adminId, announcement.getEvent().getOrganization().getId());

        announcement.setContent(request.content().trim());
        announcement.setPinned(request.pinned());
        announcement = announcementRepository.save(announcement);
        auditService.log(adminId, "EVENT_ANNOUNCEMENT_UPDATED", "EventAnnouncement", announcementId);

        return toResponse(announcement);
    }

    @Transactional
    public void delete(UUID adminId, UUID announcementId) {
        EventAnnouncement announcement = findAnnouncement(announcementId);
        organizationService.verifyAdmin(adminId, announcement.getEvent().getOrganization().getId());

        announcementRepository.delete(announcement);
        auditService.log(adminId, "EVENT_ANNOUNCEMENT_DELETED", "EventAnnouncement", announcementId);
    }

    private void notifyRegistered(Event event, EventAnnouncement announcement) {
        eventSignupRepository.findByEventIdAndStatusIn(event.getId(),
                List.of(SignupStatus.REGISTERED, SignupStatus.WAITLISTED))
                .forEach(signup -> notificationService.saveNotification(
                        signup.getUser(), NotificationType.EVENT_ANNOUNCEMENT,
                        "Annonce : " + event.getTitle(),
                        announcement.getContent().length() > 100
                                ? announcement.getContent().substring(0, 100) + "…"
                                : announcement.getContent(),
                        "/events/" + event.getId()));
    }

    private EventAnnouncement findAnnouncement(UUID id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Annonce non trouvée"));
    }

    private AnnouncementResponse toResponse(EventAnnouncement a) {
        User author = a.getAuthor();
        return new AnnouncementResponse(a.getId(), a.getEvent().getId(),
                author.getId(), author.getFirstName(), author.getLastName(),
                a.getContent(), a.isPinned(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
