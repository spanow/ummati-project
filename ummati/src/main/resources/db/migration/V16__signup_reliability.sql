-- V16 : fiabilité bénévole — statut NO_SHOW (absence) + annulation tardive.
ALTER TABLE event_signups DROP CONSTRAINT event_signups_status_check;
ALTER TABLE event_signups ADD CONSTRAINT event_signups_status_check
    CHECK (status IN ('REGISTERED', 'WAITLISTED', 'CANCELLED', 'ATTENDED', 'NO_SHOW'));

ALTER TABLE event_signups ADD COLUMN late_cancel BOOLEAN NOT NULL DEFAULT FALSE;
