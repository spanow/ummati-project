package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.event.CommentResponse;
import orga.takwa.ummati.dto.event.CreateCommentRequest;
import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.EventComment;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.EventStatus;
import orga.takwa.ummati.entity.enums.MembershipRole;
import orga.takwa.ummati.entity.enums.MembershipStatus;
import orga.takwa.ummati.entity.enums.SignupStatus;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.EventCommentRepository;
import orga.takwa.ummati.repository.EventSignupRepository;
import orga.takwa.ummati.repository.MembershipRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EventCommentService {

    private final EventCommentRepository commentRepository;
    private final EventSignupRepository eventSignupRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final AuditService auditService;

    public EventCommentService(EventCommentRepository commentRepository,
                               EventSignupRepository eventSignupRepository,
                               MembershipRepository membershipRepository,
                               UserRepository userRepository,
                               EventService eventService,
                               AuditService auditService) {
        this.commentRepository = commentRepository;
        this.eventSignupRepository = eventSignupRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.auditService = auditService;
    }

    @Transactional
    public CommentResponse create(UUID userId, UUID eventId, CreateCommentRequest request) {
        Event event = eventService.findEvent(eventId);

        if (event.getStatus() != EventStatus.PUBLISHED && event.getStatus() != EventStatus.COMPLETED) {
            throw new BusinessRuleException("Les commentaires ne sont disponibles que sur les événements publiés ou terminés");
        }

        boolean isParticipant = eventSignupRepository.findByEventIdAndUserId(eventId, userId)
                .map(s -> s.getStatus() == SignupStatus.REGISTERED
                        || s.getStatus() == SignupStatus.WAITLISTED
                        || s.getStatus() == SignupStatus.ATTENDED)
                .orElse(false);

        if (!isParticipant) {
            throw new ForbiddenException("Seuls les participants inscrits peuvent commenter");
        }

        User author = userRepository.getReferenceById(userId);
        EventComment comment = new EventComment();
        comment.setEvent(event);
        comment.setAuthor(author);
        comment.setContent(request.content().trim());
        comment = commentRepository.save(comment);
        auditService.log(userId, "EVENT_COMMENT_CREATED", "EventComment", comment.getId());

        return toResponse(comment);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> list(UUID eventId, Pageable pageable) {
        eventService.findEvent(eventId);
        return commentRepository.findByEventIdOrderByCreatedAtAsc(eventId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void delete(UUID actorId, UUID commentId) {
        EventComment comment = findComment(commentId);
        UUID orgId = comment.getEvent().getOrganization().getId();
        boolean isAuthor = comment.getAuthor().getId().equals(actorId);
        boolean isOrgAdmin = membershipRepository.findByUserIdAndOrganizationId(actorId, orgId)
                .map(m -> m.getRole() == MembershipRole.ADMIN && m.getStatus() == MembershipStatus.ACTIVE)
                .orElse(false);

        if (!isAuthor && !isOrgAdmin) {
            throw new ForbiddenException("Vous ne pouvez pas supprimer ce commentaire");
        }

        commentRepository.delete(comment);
        auditService.log(actorId, "EVENT_COMMENT_DELETED", "EventComment", commentId);
    }

    private EventComment findComment(UUID id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire non trouvé"));
    }

    CommentResponse toResponse(EventComment c) {
        User author = c.getAuthor();
        return new CommentResponse(c.getId(), c.getEvent().getId(),
                author.getId(), author.getFirstName(), author.getLastName(), author.getPhotoUrl(),
                c.getContent(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
