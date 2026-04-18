-- V5 : Tables events, event_required_skills, event_signups (H2 compatible)
CREATE TABLE events (
    id                    UUID         PRIMARY KEY DEFAULT random_uuid(),
    organization_id       UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    title                 VARCHAR(255) NOT NULL,
    description           CLOB         NOT NULL,
    objectives            CLOB,
    type                  VARCHAR(30)  NOT NULL,
    location_name         VARCHAR(255),
    location_address      VARCHAR(255),
    location_city         VARCHAR(100) NOT NULL,
    location_zip          VARCHAR(10),
    location_lat          DECIMAL(10,8),
    location_lng          DECIMAL(11,8),
    is_online             BOOLEAN      NOT NULL DEFAULT FALSE,
    online_link           VARCHAR(500),
    start_date            TIMESTAMP    NOT NULL,
    end_date              TIMESTAMP    NOT NULL,
    registration_deadline TIMESTAMP,
    max_participants      INT,
    min_age               INT,
    status                VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    cancellation_reason   CLOB,
    created_by            UUID         NOT NULL REFERENCES users(id),
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_events_org ON events (organization_id);
CREATE INDEX idx_events_status_date ON events (status, start_date);
CREATE INDEX idx_events_city ON events (location_city);
CREATE INDEX idx_events_type ON events (type);

CREATE TABLE event_required_skills (
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, skill_id)
);

CREATE TABLE event_signups (
    id            UUID        PRIMARY KEY DEFAULT random_uuid(),
    event_id      UUID        NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status        VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    registered_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at  TIMESTAMP,
    attended_at   TIMESTAMP,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (event_id, user_id)
);

CREATE INDEX idx_signups_event ON event_signups (event_id);
CREATE INDEX idx_signups_user ON event_signups (user_id);
CREATE INDEX idx_signups_status ON event_signups (status);

