-- H2-compatible V9
CREATE TABLE event_announcements (
    id         UUID        PRIMARY KEY DEFAULT random_uuid(),
    event_id   UUID        NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    author_id  UUID        NOT NULL REFERENCES users(id),
    content    TEXT        NOT NULL,
    pinned     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE event_comments (
    id         UUID        PRIMARY KEY DEFAULT random_uuid(),
    event_id   UUID        NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    author_id  UUID        NOT NULL REFERENCES users(id),
    content    TEXT        NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW()
);
