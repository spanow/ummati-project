-- V14 : Occurrences d'événements (créneaux / récurrence).
-- Un événement devient une « série » portant 1..N créneaux datés (event_occurrences).
-- L'inscription, la présence, les heures et le no-show passent au niveau occurrence.

CREATE TABLE event_occurrences (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id              UUID         NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    label                 VARCHAR(255),
    start_date            TIMESTAMP    NOT NULL,
    end_date              TIMESTAMP    NOT NULL,
    registration_deadline TIMESTAMP,
    max_participants      INT,
    status                VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                          CHECK (status IN ('DRAFT','PUBLISHED','CANCELLED','COMPLETED')),
    cancellation_reason   TEXT,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    CHECK (end_date > start_date),
    CHECK (registration_deadline IS NULL OR registration_deadline <= start_date)
);

CREATE INDEX idx_occurrences_event   ON event_occurrences (event_id);
CREATE INDEX idx_occurrences_start   ON event_occurrences (start_date);
CREATE INDEX idx_occurrences_status  ON event_occurrences (status);

-- Backfill : une occurrence miroir par événement existant (copie dates / capacité / statut).
INSERT INTO event_occurrences (id, event_id, label, start_date, end_date,
                               registration_deadline, max_participants, status,
                               cancellation_reason, created_at, updated_at)
SELECT gen_random_uuid(), e.id, NULL, e.start_date, e.end_date,
       e.registration_deadline, e.max_participants, e.status,
       e.cancellation_reason, e.created_at, e.updated_at
FROM events e;

-- Rattacher chaque inscription existante à l'occurrence miroir de son événement.
ALTER TABLE event_signups ADD COLUMN occurrence_id UUID
    REFERENCES event_occurrences(id) ON DELETE CASCADE;

UPDATE event_signups s
SET occurrence_id = o.id
FROM event_occurrences o
WHERE o.event_id = s.event_id;

ALTER TABLE event_signups ALTER COLUMN occurrence_id SET NOT NULL;

-- L'unicité passe au niveau occurrence : un bénévole peut s'inscrire à plusieurs créneaux d'une série.
ALTER TABLE event_signups DROP CONSTRAINT event_signups_event_id_user_id_key;
ALTER TABLE event_signups ADD CONSTRAINT event_signups_occurrence_user_key
    UNIQUE (occurrence_id, user_id);

CREATE INDEX idx_signups_occurrence ON event_signups (occurrence_id);
