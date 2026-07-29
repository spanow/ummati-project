-- V21 : rétention des bénévoles.
--
-- Trois mécanismes qui donnent une raison de revenir, et le réglage qui les rend
-- tenables : sans préférences ni désinscription, tripler les sources de notification
-- ferait fuir les gens au lieu de les retenir.

-- --- Missions mises de côté ---------------------------------------------------
CREATE TABLE event_favorites (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_id   UUID      NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, event_id)
);
CREATE INDEX idx_favorites_user ON event_favorites (user_id, created_at DESC);

-- --- Associations suivies -----------------------------------------------------
CREATE TABLE organization_follows (
    id              UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID      NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, organization_id)
);
CREATE INDEX idx_follows_org ON organization_follows (organization_id);

-- --- Alertes : une recherche sauvegardée, rejouée périodiquement --------------
-- domains et types sont des listes courtes et fermées, stockées en CSV : une table
-- de liaison pour trois valeurs coûterait plus qu'elle ne rapporte ici.
CREATE TABLE mission_alerts (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label        VARCHAR(120) NOT NULL,
    city         VARCHAR(100),
    lat          DECIMAL(10,8),
    lng          DECIMAL(11,8),
    radius_km    INT,
    domains      TEXT,
    types        TEXT,
    frequency    VARCHAR(10)  NOT NULL DEFAULT 'WEEKLY'
                 CHECK (frequency IN ('DAILY', 'WEEKLY')),
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Borne basse de la prochaine recherche : on ne renvoie jamais deux fois la
    -- même mission, même si la tâche est rejouée.
    last_sent_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_alerts_due ON mission_alerts (enabled, last_sent_at);

-- --- Préférences de notification ----------------------------------------------
-- Une ligne par utilisateur, créée à la volée au premier accès. Les emails
-- strictement transactionnels (vérification d'adresse, réinitialisation de mot de
-- passe, confirmation d'inscription à une mission) ne figurent pas ici : ils ne
-- sont pas désactivables, c'est ce que l'utilisateur a explicitement demandé.
CREATE TABLE notification_preferences (
    user_id            UUID      PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    email_new_missions BOOLEAN   NOT NULL DEFAULT TRUE,
    email_reminders    BOOLEAN   NOT NULL DEFAULT TRUE,
    email_memberships  BOOLEAN   NOT NULL DEFAULT TRUE,
    email_announcements BOOLEAN  NOT NULL DEFAULT TRUE,
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Jeton de désinscription : permet le lien « se désabonner » en un clic depuis un
-- email, sans connexion. Aléatoire et propre à l'utilisateur.
ALTER TABLE users ADD COLUMN unsubscribe_token VARCHAR(64);
UPDATE users SET unsubscribe_token = replace(gen_random_uuid()::text, '-', '')
WHERE unsubscribe_token IS NULL;
CREATE UNIQUE INDEX idx_users_unsubscribe_token ON users (unsubscribe_token);

-- --- Nouveaux types de notification -------------------------------------------
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
        'MISSION_ALERT','ORG_NEW_EVENT','FAVORITE_CLOSING',
        'GENERAL'
    ));
