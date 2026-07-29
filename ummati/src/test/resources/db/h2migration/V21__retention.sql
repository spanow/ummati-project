-- Mirror H2 de la migration Postgres V21 (rétention).
-- NB : non exécuté par le profil h2test (Flyway désactivé, schéma généré par Hibernate
-- create-drop à partir des entités) — conservé pour parité documentaire.

CREATE TABLE event_favorites (
    id         UUID      PRIMARY KEY DEFAULT random_uuid(),
    user_id    UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_id   UUID      NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, event_id)
);
CREATE INDEX idx_favorites_user ON event_favorites (user_id, created_at DESC);

CREATE TABLE organization_follows (
    id              UUID      PRIMARY KEY DEFAULT random_uuid(),
    user_id         UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID      NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, organization_id)
);
CREATE INDEX idx_follows_org ON organization_follows (organization_id);

CREATE TABLE mission_alerts (
    id           UUID         PRIMARY KEY DEFAULT random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label        VARCHAR(120) NOT NULL,
    city         VARCHAR(100),
    lat          DECIMAL(10,8),
    lng          DECIMAL(11,8),
    radius_km    INT,
    domains      CLOB,
    types        CLOB,
    frequency    VARCHAR(10)  NOT NULL DEFAULT 'WEEKLY'
                 CHECK (frequency IN ('DAILY', 'WEEKLY')),
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    last_sent_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_alerts_due ON mission_alerts (enabled, last_sent_at);

CREATE TABLE notification_preferences (
    user_id             UUID      PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    email_new_missions  BOOLEAN   NOT NULL DEFAULT TRUE,
    email_reminders     BOOLEAN   NOT NULL DEFAULT TRUE,
    email_memberships   BOOLEAN   NOT NULL DEFAULT TRUE,
    email_announcements BOOLEAN   NOT NULL DEFAULT TRUE,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE users ADD COLUMN unsubscribe_token VARCHAR(64);
CREATE UNIQUE INDEX idx_users_unsubscribe_token ON users (unsubscribe_token);

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check
    CHECK (type IN (
        'WELCOME','EMAIL_VERIFIED',
        'ONG_SUBMITTED','ONG_VALIDATED','ONG_REJECTED',
        'MEMBERSHIP_REQUESTED','MEMBERSHIP_ACCEPTED','MEMBERSHIP_REJECTED',
        'EVENT_PUBLISHED','EVENT_REMINDER_7D','EVENT_REMINDER_1D',
        'EVENT_CANCELLED','EVENT_COMPLETED',
        'SIGNUP_CONFIRMED','SIGNUP_WAITLISTED','SIGNUP_PROMOTED',
        'FEEDBACK_REQUESTED',
        'EVENT_ANNOUNCEMENT','ORG_ANNOUNCEMENT',
        'EVENT_PHOTOS_REQUESTED',
        'MISSION_ALERT','ORG_NEW_EVENT','FAVORITE_CLOSING',
        'GENERAL'
    ));
