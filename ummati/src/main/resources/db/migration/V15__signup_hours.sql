-- V15 : heures de bénévolat validées et stockées par inscription.
-- Renseignées à la validation de présence (défaut = durée du créneau, ajustable par l'ONG),
-- puis figées : elles ne sont jamais recalculées à partir des dates d'événement.
ALTER TABLE event_signups ADD COLUMN hours_validated DECIMAL(5,2);
