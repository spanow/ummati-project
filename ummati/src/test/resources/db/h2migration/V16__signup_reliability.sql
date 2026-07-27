-- Mirror H2 de la migration Postgres V16.
-- H2 (profil de test) n'a pas de CHECK sur event_signups.status : rien à modifier de ce côté.
ALTER TABLE event_signups ADD COLUMN late_cancel BOOLEAN NOT NULL DEFAULT FALSE;
