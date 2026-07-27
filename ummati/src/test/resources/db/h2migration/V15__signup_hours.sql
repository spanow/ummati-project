-- Mirror H2 de la migration Postgres V15 (heures de bénévolat validées).
-- NB : non exécuté par le profil h2test (Flyway désactivé, schéma généré par Hibernate
-- create-drop à partir des entités) — conservé pour parité documentaire.

ALTER TABLE event_signups ADD COLUMN hours_validated DECIMAL(5,2);
