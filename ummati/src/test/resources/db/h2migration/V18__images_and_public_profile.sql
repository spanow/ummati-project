-- Mirror H2 de la migration Postgres V18 (visuels + profil bénévole public).
-- NB : non exécuté par le profil h2test (Flyway désactivé, schéma généré par Hibernate
-- create-drop à partir des entités) — conservé pour parité documentaire.

ALTER TABLE events ADD COLUMN cover_url VARCHAR(500);

CREATE TABLE event_photos (
    id          UUID         PRIMARY KEY DEFAULT random_uuid(),
    event_id    UUID         NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    url         VARCHAR(500) NOT NULL,
    caption     VARCHAR(500),
    position    INT          NOT NULL DEFAULT 0,
    uploaded_by UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_event_photos_event ON event_photos (event_id, position);

ALTER TABLE users ADD COLUMN profile_public BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_events_coordinates ON events (location_lat, location_lng);
