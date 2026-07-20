-- Mirror H2 de la migration Postgres V14 (occurrences d'événements).
-- NB : non exécuté par le profil h2test (Flyway désactivé, schéma généré par Hibernate
-- create-drop à partir des entités) — conservé pour parité documentaire.

CREATE TABLE event_occurrences (
    id                    UUID         PRIMARY KEY DEFAULT random_uuid(),
    event_id              UUID         NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    label                 VARCHAR(255),
    start_date            TIMESTAMP    NOT NULL,
    end_date              TIMESTAMP    NOT NULL,
    registration_deadline TIMESTAMP,
    max_participants      INT,
    status                VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    cancellation_reason   CLOB,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_occurrences_event   ON event_occurrences (event_id);
CREATE INDEX idx_occurrences_start   ON event_occurrences (start_date);
CREATE INDEX idx_occurrences_status  ON event_occurrences (status);

INSERT INTO event_occurrences (id, event_id, label, start_date, end_date,
                               registration_deadline, max_participants, status,
                               cancellation_reason, created_at, updated_at)
SELECT random_uuid(), e.id, NULL, e.start_date, e.end_date,
       e.registration_deadline, e.max_participants, e.status,
       e.cancellation_reason, e.created_at, e.updated_at
FROM events e;

ALTER TABLE event_signups ADD COLUMN occurrence_id UUID;
UPDATE event_signups s SET occurrence_id =
    (SELECT o.id FROM event_occurrences o WHERE o.event_id = s.event_id);
ALTER TABLE event_signups ALTER COLUMN occurrence_id SET NOT NULL;
ALTER TABLE event_signups ADD CONSTRAINT fk_signups_occurrence
    FOREIGN KEY (occurrence_id) REFERENCES event_occurrences(id) ON DELETE CASCADE;
ALTER TABLE event_signups DROP CONSTRAINT IF EXISTS event_signups_event_id_user_id_key;
ALTER TABLE event_signups ADD CONSTRAINT event_signups_occurrence_user_key
    UNIQUE (occurrence_id, user_id);

CREATE INDEX idx_signups_occurrence ON event_signups (occurrence_id);
