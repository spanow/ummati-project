package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.event.CommentResponse;
import orga.takwa.ummati.dto.event.CreateCommentRequest;
import orga.takwa.ummati.entity.*;
import orga.takwa.ummati.entity.enums.*;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.repository.EventCommentRepository;
import orga.takwa.ummati.repository.EventSignupRepository;
import orga.takwa.ummati.repository.MembershipRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventCommentServiceTest {

    @Mock private EventCommentRepository commentRepository;
    @Mock private EventSignupRepository eventSignupRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventService eventService;
    @Mock private AuditService auditService;

    @InjectMocks
    private EventCommentService commentService;

    private UUID userId;
    private UUID eventId;
    private Event publishedEvent;
    private Organization org;
    private User participant;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        org = new Organization();
        org.setId(UUID.randomUUID());
        org.setName("Test ONG");
        org.setSlug("test-ong");

        participant = new User();
        participant.setId(userId);
        participant.setFirstName("Alice");
        participant.setLastName("Dupont");

        publishedEvent = new Event();
        publishedEvent.setId(eventId);
        publishedEvent.setOrganization(org);
        publishedEvent.setTitle("Maraude");
        publishedEvent.setStatus(EventStatus.PUBLISHED);
        publishedEvent.setRequiredSkills(new java.util.HashSet<>());
    }

    @Test
    void create_shouldSucceed_whenRegistered() {
        EventSignup signup = new EventSignup();
        signup.setUser(participant);
        signup.setEvent(publishedEvent);
        signup.setStatus(SignupStatus.REGISTERED);

        when(eventService.findEvent(eventId)).thenReturn(publishedEvent);
        when(eventSignupRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.of(signup));
        when(userRepository.getReferenceById(userId)).thenReturn(participant);
        when(commentRepository.save(any())).thenAnswer(inv -> {
            EventComment c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CommentResponse result = commentService.create(userId, eventId, new CreateCommentRequest("Hâte d'y être !"));

        assertThat(result.content()).isEqualTo("Hâte d'y être !");
        assertThat(result.authorFirstName()).isEqualTo("Alice");
        verify(auditService).log(eq(userId), eq("EVENT_COMMENT_CREATED"), eq("EventComment"), any());
    }

    @Test
    void create_shouldSucceed_whenAttended() {
        EventSignup signup = new EventSignup();
        signup.setUser(participant);
        signup.setEvent(publishedEvent);
        signup.setStatus(SignupStatus.ATTENDED);
        publishedEvent.setStatus(EventStatus.COMPLETED);

        when(eventService.findEvent(eventId)).thenReturn(publishedEvent);
        when(eventSignupRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.of(signup));
        when(userRepository.getReferenceById(userId)).thenReturn(participant);
        when(commentRepository.save(any())).thenAnswer(inv -> {
            EventComment c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CommentResponse result = commentService.create(userId, eventId, new CreateCommentRequest("Excellente maraude !"));

        assertThat(result.content()).isEqualTo("Excellente maraude !");
    }

    @Test
    void create_shouldFail_whenNotInscrit() {
        when(eventService.findEvent(eventId)).thenReturn(publishedEvent);
        when(eventSignupRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(userId, eventId, new CreateCommentRequest("Commentaire")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("inscrits");
    }

    @Test
    void create_shouldFail_whenCancelledSignup() {
        EventSignup signup = new EventSignup();
        signup.setStatus(SignupStatus.CANCELLED);
        signup.setUser(participant);
        signup.setEvent(publishedEvent);

        when(eventService.findEvent(eventId)).thenReturn(publishedEvent);
        when(eventSignupRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.of(signup));

        assertThatThrownBy(() -> commentService.create(userId, eventId, new CreateCommentRequest("Commentaire")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("inscrits");
    }

    @Test
    void create_shouldFail_whenEventNotPublished() {
        publishedEvent.setStatus(EventStatus.DRAFT);
        when(eventService.findEvent(eventId)).thenReturn(publishedEvent);

        assertThatThrownBy(() -> commentService.create(userId, eventId, new CreateCommentRequest("Commentaire")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("publiés ou terminés");
    }

    @Test
    void delete_shouldSucceed_whenAuthor() {
        EventComment comment = buildComment(participant);
        UUID commentId = comment.getId();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        commentService.delete(userId, commentId);

        verify(commentRepository).delete(comment);
        verify(auditService).log(eq(userId), eq("EVENT_COMMENT_DELETED"), eq("EventComment"), eq(commentId));
    }

    @Test
    void delete_shouldSucceed_whenOrgAdmin() {
        UUID otherId = UUID.randomUUID();
        User other = new User();
        other.setId(otherId);
        EventComment comment = buildComment(other);
        UUID commentId = comment.getId();

        Membership adminMembership = new Membership();
        adminMembership.setRole(MembershipRole.ADMIN);
        adminMembership.setStatus(MembershipStatus.ACTIVE);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(membershipRepository.findByUserIdAndOrganizationId(userId, org.getId()))
                .thenReturn(Optional.of(adminMembership));

        commentService.delete(userId, commentId);

        verify(commentRepository).delete(comment);
    }

    @Test
    void delete_shouldFail_whenNeitherAuthorNorAdmin() {
        UUID strangerId = UUID.randomUUID();
        User stranger = new User();
        stranger.setId(strangerId);
        EventComment comment = buildComment(stranger);

        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(membershipRepository.findByUserIdAndOrganizationId(userId, org.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(userId, comment.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("supprimer");
    }

    @Test
    void list_returnsPagedComments() {
        EventComment c1 = buildComment(participant);
        when(eventService.findEvent(eventId)).thenReturn(publishedEvent);
        when(commentRepository.findByEventIdOrderByCreatedAtAsc(eq(eventId), any()))
                .thenReturn(new PageImpl<>(List.of(c1)));

        var result = commentService.list(eventId, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).authorFirstName()).isEqualTo("Alice");
    }

    private EventComment buildComment(User author) {
        EventComment c = new EventComment();
        c.setId(UUID.randomUUID());
        c.setEvent(publishedEvent);
        c.setAuthor(author);
        c.setContent("Commentaire de test");
        return c;
    }
}
