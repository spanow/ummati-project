-- V6 : event_feedbacks, documents, notifications, verification_tokens, audit_logs

CREATE TABLE event_feedbacks (
    id           UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id     UUID    NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id      UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating       INT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      TEXT,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (event_id, user_id)
);

CREATE INDEX idx_feedbacks_event ON event_feedbacks (event_id);

CREATE TABLE documents (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_type    VARCHAR(20)  NOT NULL CHECK (owner_type IN ('ORGANIZATION','EVENT')),
    owner_id      UUID         NOT NULL,
    name          VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_path     VARCHAR(500) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    file_size     BIGINT       NOT NULL,
    uploaded_by   UUID         NOT NULL REFERENCES users(id),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documents_owner ON documents (owner_type, owner_id);

CREATE TABLE notifications (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(30)  NOT NULL
               CHECK (type IN ('WELCOME','EMAIL_VERIFIED','ONG_SUBMITTED','ONG_VALIDATED',
                               'ONG_REJECTED','MEMBERSHIP_REQUESTED','MEMBERSHIP_ACCEPTED',
                               'MEMBERSHIP_REJECTED','EVENT_PUBLISHED','EVENT_REMINDER_7D',
                               'EVENT_REMINDER_1D','EVENT_CANCELLED','EVENT_COMPLETED',
                               'SIGNUP_CONFIRMED','SIGNUP_WAITLISTED','SIGNUP_PROMOTED',
                               'FEEDBACK_REQUESTED','GENERAL')),
    title      VARCHAR(255) NOT NULL,
    message    TEXT         NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    link       VARCHAR(500),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifs_user_read ON notifications (user_id, is_read);

CREATE TABLE verification_tokens (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    type       VARCHAR(20)  NOT NULL
               CHECK (type IN ('EMAIL_VERIFICATION','PASSWORD_RESET')),
    expires_at TIMESTAMP    NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vtokens_token ON verification_tokens (token);
CREATE INDEX idx_vtokens_user_type ON verification_tokens (user_id, type);

CREATE TABLE audit_logs (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID        REFERENCES users(id),
    action      VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id   UUID        NOT NULL,
    old_value   JSONB,
    new_value   JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_logs (actor_id);
CREATE INDEX idx_audit_date ON audit_logs (created_at);

