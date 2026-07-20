-- Mirror H2 de la migration Postgres V17 (questionnaire d'adhésion).
-- Non exécuté par le profil h2test (schéma généré par Hibernate) — parité documentaire.

CREATE TABLE membership_questions (
    id              UUID         PRIMARY KEY DEFAULT random_uuid(),
    organization_id UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    label           VARCHAR(500) NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    options         CLOB,
    required        BOOLEAN      NOT NULL DEFAULT FALSE,
    position        INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_membership_questions_org ON membership_questions (organization_id);

CREATE TABLE membership_answers (
    id            UUID      PRIMARY KEY DEFAULT random_uuid(),
    membership_id UUID      NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,
    question_id   UUID      NOT NULL REFERENCES membership_questions(id) ON DELETE CASCADE,
    answer_value  CLOB,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (membership_id, question_id)
);
CREATE INDEX idx_membership_answers_membership ON membership_answers (membership_id);
