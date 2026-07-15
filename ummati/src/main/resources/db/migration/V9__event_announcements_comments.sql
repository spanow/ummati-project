-- V9 : Annonces admin et commentaires participants sur les événements
CREATE TABLE event_announcements (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id   UUID        NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    author_id  UUID        NOT NULL REFERENCES users(id),
    content    TEXT        NOT NULL,
    pinned     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_announcements_event ON event_announcements (event_id);

CREATE TABLE event_comments (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id   UUID        NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    author_id  UUID        NOT NULL REFERENCES users(id),
    content    TEXT        NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_event ON event_comments (event_id);
