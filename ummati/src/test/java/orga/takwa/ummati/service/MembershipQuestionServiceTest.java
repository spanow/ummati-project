package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.membership.*;
import orga.takwa.ummati.entity.Membership;
import orga.takwa.ummati.entity.MembershipQuestion;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.enums.QuestionType;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.repository.MembershipAnswerRepository;
import orga.takwa.ummati.repository.MembershipQuestionRepository;
import orga.takwa.ummati.repository.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipQuestionServiceTest {

    @Mock private MembershipQuestionRepository questionRepository;
    @Mock private MembershipAnswerRepository answerRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private OrganizationService organizationService;
    @Mock private AuditService auditService;

    @InjectMocks
    private MembershipQuestionService service;

    private UUID adminId;
    private UUID orgId;
    private Organization org;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        org = new Organization();
        org.setId(orgId);
        org.setName("ONG Test");
    }

    @Test
    void createQuestion_shouldPersist_forBooleanQuestion() {
        doNothing().when(organizationService).verifyAdmin(adminId, orgId);
        when(organizationService.findOrg(orgId)).thenReturn(org);
        when(questionRepository.save(any(MembershipQuestion.class))).thenAnswer(inv -> {
            MembershipQuestion q = inv.getArgument(0);
            q.setId(UUID.randomUUID());
            return q;
        });

        QuestionResponse r = service.createQuestion(adminId, orgId,
                new CreateQuestionRequest("Disponible le week-end ?", "BOOLEAN", null, true));

        assertThat(r.label()).isEqualTo("Disponible le week-end ?");
        assertThat(r.type()).isEqualTo("BOOLEAN");
        assertThat(r.required()).isTrue();
        verify(auditService).log(eq(adminId), eq("MEMBERSHIP_QUESTION_CREATED"), eq("MembershipQuestion"), any());
    }

    @Test
    void createQuestion_shouldFail_whenSingleChoiceHasLessThanTwoOptions() {
        doNothing().when(organizationService).verifyAdmin(adminId, orgId);
        when(organizationService.findOrg(orgId)).thenReturn(org);

        assertThatThrownBy(() -> service.createQuestion(adminId, orgId,
                new CreateQuestionRequest("Votre choix", "SINGLE_CHOICE", List.of("Un seul"), false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("2 options");
    }

    @Test
    void saveAnswers_shouldFail_whenRequiredQuestionUnanswered() {
        MembershipQuestion q = requiredTextQuestion();
        Membership m = membership();
        when(questionRepository.findByOrganizationIdOrderByPositionAsc(orgId)).thenReturn(List.of(q));

        assertThatThrownBy(() -> service.saveAnswers(m, List.of()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Merci de répondre");
    }

    @Test
    void saveAnswers_shouldStoreProvidedAnswers() {
        MembershipQuestion q = requiredTextQuestion();
        Membership m = membership();
        when(questionRepository.findByOrganizationIdOrderByPositionAsc(orgId)).thenReturn(List.of(q));

        service.saveAnswers(m, List.of(new AnswerInput(q.getId(), "Pour aider ma communauté")));

        verify(answerRepository).deleteByMembershipId(m.getId());
        verify(answerRepository).save(argThat(a ->
                "Pour aider ma communauté".equals(a.getValue()) && a.getQuestion() == q && a.getMembership() == m));
    }

    @Test
    void saveAnswers_shouldNoOp_whenNoQuestions() {
        Membership m = membership();
        when(questionRepository.findByOrganizationIdOrderByPositionAsc(orgId)).thenReturn(List.of());

        service.saveAnswers(m, List.of());

        verify(answerRepository, never()).save(any());
        verify(answerRepository, never()).deleteByMembershipId(any());
    }

    private MembershipQuestion requiredTextQuestion() {
        MembershipQuestion q = new MembershipQuestion();
        q.setId(UUID.randomUUID());
        q.setOrganization(org);
        q.setLabel("Pourquoi nous rejoindre ?");
        q.setType(QuestionType.TEXT);
        q.setRequired(true);
        return q;
    }

    private Membership membership() {
        Membership m = new Membership();
        m.setId(UUID.randomUUID());
        m.setOrganization(org);
        return m;
    }
}
