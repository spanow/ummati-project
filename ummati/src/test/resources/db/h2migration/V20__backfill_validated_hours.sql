-- Mirror H2 de la migration Postgres V20 (reprise des heures des présences anciennes).
-- NB : non exécuté par le profil h2test (Flyway désactivé, schéma généré par Hibernate
-- create-drop à partir des entités) — conservé pour parité documentaire.

UPDATE event_signups s
SET hours_validated = (
        SELECT LEAST(
                 ROUND(GREATEST(DATEDIFF('MINUTE', o.start_date, o.end_date), 0) / 60.0, 2),
                 -- Plafond de la colonne DECIMAL(5,2) : sans lui, un créneau aberrant
                 -- fait échouer toute la migration.
                 999.99)
        FROM event_occurrences o
        WHERE o.id = s.occurrence_id)
WHERE s.status = 'ATTENDED'
  AND s.hours_validated IS NULL
  AND s.occurrence_id IS NOT NULL;

UPDATE event_signups
SET hours_validated = 0
WHERE status = 'ATTENDED'
  AND hours_validated IS NULL;
