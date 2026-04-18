-- V4 : Table memberships
CREATE TABLE memberships (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL DEFAULT 'MEMBER'
                    CHECK (role IN ('MEMBER','ADMIN','ACCOUNTANT')),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','ACTIVE','REJECTED','LEFT')),
    motivation      TEXT,
    rejected_at     TIMESTAMP,
    joined_at       TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, organization_id)
);

CREATE INDEX idx_memberships_user ON memberships (user_id);
CREATE INDEX idx_memberships_org ON memberships (organization_id);
CREATE INDEX idx_memberships_status ON memberships (status);

