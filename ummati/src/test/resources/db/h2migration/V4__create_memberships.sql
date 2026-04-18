-- V4 : Table memberships (H2 compatible)
CREATE TABLE memberships (
    id              UUID        PRIMARY KEY DEFAULT random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    motivation      CLOB,
    rejected_at     TIMESTAMP,
    joined_at       TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, organization_id)
);

CREATE INDEX idx_memberships_user ON memberships (user_id);
CREATE INDEX idx_memberships_org ON memberships (organization_id);
CREATE INDEX idx_memberships_status ON memberships (status);

