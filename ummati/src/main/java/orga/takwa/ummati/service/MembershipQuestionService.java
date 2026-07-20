package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.membership.*;
import orga.takwa.ummati.entity.Membership;
import orga.takwa.ummati.entity.MembershipAnswer;
import orga.takwa.ummati.entity.MembershipQuestion;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.enums.QuestionType;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.MembershipAnswerRepository;
import orga.takwa.ummati.repository.MembershipQuestionRepository;
import orga.takwa.ummati.repository.MembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Questionnaire d'adhésion configurable par ONG : gestion des questions (admin),
 * stockage et validation des réponses des candidats, consultation par l'admin.
 */
@Service
public class MembershipQuestionService {

    private final MembershipQuestionRepository questionRepository;
    private final MembershipAnswerRepository answerRepository;
    private final MembershipRepository membershipRepository;
    private final OrganizationService organizationService;
    private final AuditService auditService;

    public MembershipQuestionService(MembershipQuestionRepository questionRepository,
                                     MembershipAnswerRepository answerRepository,
                                     MembershipRepository membershipRepository,
                                     OrganizationService organizationService, AuditService auditService) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.membershipRepository = membershipRepository;
        this.organizationService = organizationService;
        this.auditService = auditService;
    }

    // --- Gestion des questions (admin ONG) ---

    @Transactional
    public QuestionResponse createQuestion(UUID adminId, UUID orgId, CreateQuestionRequest request) {
        organizationService.verifyAdmin(adminId, orgId);
        Organization org = organizationService.findOrg(orgId);
        QuestionType type = parseType(request.type());

        MembershipQuestion q = new MembershipQuestion();
        q.setOrganization(org);
        q.setLabel(request.label().trim());
        q.setType(type);
        q.setRequired(request.required());
        q.setPosition((int) questionRepository.countByOrganizationId(orgId));

        if (type == QuestionType.SINGLE_CHOICE) {
            List<String> cleaned = request.options() == null ? List.of()
                    : request.options().stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).toList();
            if (cleaned.size() < 2) {
                throw new BusinessRuleException("Une question à choix nécessite au moins 2 options");
            }
            q.setOptions(String.join("\n", cleaned));
        }

        q = questionRepository.save(q);
        auditService.log(adminId, "MEMBERSHIP_QUESTION_CREATED", "MembershipQuestion", q.getId());
        return toResponse(q);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> listQuestions(UUID orgId) {
        return questionRepository.findByOrganizationIdOrderByPositionAsc(orgId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public void deleteQuestion(UUID adminId, UUID questionId) {
        MembershipQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question non trouvée"));
        organizationService.verifyAdmin(adminId, q.getOrganization().getId());
        questionRepository.delete(q);
        auditService.log(adminId, "MEMBERSHIP_QUESTION_DELETED", "MembershipQuestion", questionId);
    }

    // --- Réponses ---

    /** Valide (questions obligatoires) et enregistre les réponses d'une demande d'adhésion. */
    @Transactional
    public void saveAnswers(Membership membership, List<AnswerInput> answers) {
        UUID orgId = membership.getOrganization().getId();
        List<MembershipQuestion> questions = questionRepository.findByOrganizationIdOrderByPositionAsc(orgId);
        if (questions.isEmpty()) {
            return;
        }

        Map<UUID, String> provided = new HashMap<>();
        if (answers != null) {
            for (AnswerInput a : answers) {
                if (a.questionId() != null) provided.put(a.questionId(), a.value());
            }
        }

        for (MembershipQuestion q : questions) {
            String val = provided.get(q.getId());
            if (q.isRequired() && (val == null || val.isBlank())) {
                throw new BusinessRuleException("Merci de répondre à la question : " + q.getLabel());
            }
            if (q.getType() == QuestionType.SINGLE_CHOICE && val != null && !val.isBlank()
                    && !splitOptions(q.getOptions()).contains(val.trim())) {
                throw new BusinessRuleException("Réponse invalide pour : " + q.getLabel());
            }
        }

        // Remplace d'éventuelles réponses précédentes (re-demande après cooldown).
        answerRepository.deleteByMembershipId(membership.getId());
        for (MembershipQuestion q : questions) {
            String val = provided.get(q.getId());
            if (val == null || val.isBlank()) continue;
            MembershipAnswer ans = new MembershipAnswer();
            ans.setMembership(membership);
            ans.setQuestion(q);
            ans.setValue(val.trim());
            answerRepository.save(ans);
        }
    }

    @Transactional(readOnly = true)
    public List<AnswerResponse> getAnswers(UUID callerId, UUID membershipId) {
        Membership m = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Adhésion non trouvée"));
        organizationService.verifyAdmin(callerId, m.getOrganization().getId());
        return answerRepository.findByMembershipId(membershipId).stream()
                .map(a -> new AnswerResponse(a.getQuestion().getId(), a.getQuestion().getLabel(),
                        a.getQuestion().getType().name(), a.getValue()))
                .toList();
    }

    // --- Helpers ---

    private QuestionResponse toResponse(MembershipQuestion q) {
        return new QuestionResponse(q.getId(), q.getLabel(), q.getType().name(),
                splitOptions(q.getOptions()), q.isRequired(), q.getPosition());
    }

    private List<String> splitOptions(String options) {
        if (options == null || options.isBlank()) return List.of();
        return Arrays.stream(options.split("\n")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private QuestionType parseType(String type) {
        try {
            return QuestionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Type de question invalide (TEXT, BOOLEAN ou SINGLE_CHOICE)");
        }
    }
}
