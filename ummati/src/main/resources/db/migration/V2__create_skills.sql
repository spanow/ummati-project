-- V2 : Tables skills & user_skills
CREATE TABLE skills (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL UNIQUE,
    category   VARCHAR(50)  NOT NULL
               CHECK (category IN ('TECHNIQUE','COMMUNICATION','LOGISTIQUE','MEDICAL',
                                   'JURIDIQUE','ENSEIGNEMENT','ARTISANAT','CUISINE',
                                   'CONDUITE','LANGUE','AUTRE')),
    created_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE user_skills (
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, skill_id)
);

CREATE INDEX idx_user_skills_user ON user_skills (user_id);
CREATE INDEX idx_user_skills_skill ON user_skills (skill_id);

