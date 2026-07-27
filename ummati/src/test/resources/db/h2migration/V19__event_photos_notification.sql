-- Mirror H2 de la migration Postgres V19 (type de notification EVENT_PHOTOS_REQUESTED).
-- NB : non exécuté par le profil h2test (Flyway désactivé, schéma généré par Hibernate
-- create-drop à partir des entités) — conservé pour parité documentaire.

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check
    CHECK (type IN (
        'WELCOME','EMAIL_VERIFIED',
        'ONG_SUBMITTED','ONG_VALIDATED','ONG_REJECTED',
        'MEMBERSHIP_REQUESTED','MEMBERSHIP_ACCEPTED','MEMBERSHIP_REJECTED',
        'EVENT_PUBLISHED','EVENT_REMINDER_7D','EVENT_REMINDER_1D',
        'EVENT_CANCELLED','EVENT_COMPLETED',
        'SIGNUP_CONFIRMED','SIGNUP_WAITLISTED','SIGNUP_PROMOTED',
        'FEEDBACK_REQUESTED',
        'EVENT_ANNOUNCEMENT','ORG_ANNOUNCEMENT',
        'EVENT_PHOTOS_REQUESTED',
        'GENERAL'
    ));
