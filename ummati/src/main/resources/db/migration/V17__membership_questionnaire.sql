-- V17 : questionnaire d'adhésion configurable par ONG + réponses des candidats.

CREATE TABLE membership_questions (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    label           VARCHAR(500) NOT NULL,
    type            VARCHAR(20)  NOT NULL CHECK (type IN ('TEXT', 'BOOLEAN', 'SINGLE_CHOICE')),
    options         TEXT,
    required        BOOLEAN      NOT NULL DEFAULT FALSE,
    position        INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_membership_questions_org ON membership_questions (organization_id);

CREATE TABLE membership_answers (
    id            UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    membership_id UUID      NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,
    question_id   UUID      NOT NULL REFERENCES membership_questions(id) ON DELETE CASCADE,
    answer_value  TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (membership_id, question_id)
);
CREATE INDEX idx_membership_answers_membership ON membership_answers (membership_id);
