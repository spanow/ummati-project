-- V10 (H2) : Fix notifications type constraint + org-level announcements

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
        'GENERAL'
    ));

CREATE TABLE organization_announcements (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id      UUID        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    author_id   UUID        NOT NULL REFERENCES users(id),
    title       VARCHAR(200) NOT NULL,
    content     TEXT        NOT NULL,
    pinned      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_org_announcements_org ON organization_announcements (org_id);
