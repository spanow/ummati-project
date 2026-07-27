-- V18 : visuels (couverture d'événement, galerie post-événement) + profil bénévole public.
--
-- Les colonnes organizations.logo_url / banner_url existent depuis V3 mais n'étaient
-- alimentées par aucun endpoint : elles le sont désormais (cf. ImageController).

-- --- Image de couverture d'un événement -------------------------------------
ALTER TABLE events ADD COLUMN cover_url VARCHAR(500);

-- --- Galerie photo d'un événement (alimentée après la mission) --------------
CREATE TABLE event_photos (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID         NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    url         VARCHAR(500) NOT NULL,
    caption     VARCHAR(500),
    position    INT          NOT NULL DEFAULT 0,
    uploaded_by UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_event_photos_event ON event_photos (event_id, position);

-- --- Passeport bénévole : visibilité publique opt-in ------------------------
-- Par défaut FALSE : aucun profil existant ne devient public sans action explicite.
ALTER TABLE users ADD COLUMN profile_public BOOLEAN NOT NULL DEFAULT FALSE;

-- --- Recherche géographique des événements ----------------------------------
-- Index sur le couple lat/lng pour le pré-filtrage par bounding box.
CREATE INDEX idx_events_coordinates ON events (location_lat, location_lng);
