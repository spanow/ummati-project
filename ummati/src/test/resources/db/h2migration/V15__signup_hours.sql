-- Mirror H2 de la migration Postgres V15 (heures validées par inscription).
ALTER TABLE event_signups ADD COLUMN hours_validated DECIMAL(5,2);
