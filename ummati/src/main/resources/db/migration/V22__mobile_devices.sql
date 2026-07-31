-- V22 : socle mobile — identité de l'appareil et sessions longues.
--
-- Deux besoins que le web n'avait pas :
--   1. Le Web Push (V12) identifie un navigateur par une URL d'endpoint fournie par
--      le navigateur lui-même. Une app native n'a pas d'endpoint : elle a un jeton
--      FCM/APNs opaque. Le modèle de push_subscriptions ne peut pas l'accueillir,
--      d'où une table dédiée plutôt qu'une colonne nullable de plus.
--   2. Un refresh JWT de 7 jours non révocable est acceptable sur un navigateur que
--      l'on referme ; sur mobile il déconnecte l'utilisateur chaque semaine et ne
--      permet pas de couper l'accès d'un téléphone perdu. On stocke donc un jeton
--      opaque, rotatif et révocable.

-- --- Appareils enregistrés pour les notifications natives ---------------------
-- token est UNIQUE et non (user_id, token) : un jeton FCM appartient à une
-- installation, pas à un compte. Quand un second utilisateur se connecte sur le
-- même téléphone, la ligne doit changer de propriétaire — sans cette unicité on
-- enverrait les notifications du premier compte au second.
CREATE TABLE device_tokens (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token        VARCHAR(512) NOT NULL,
    platform     VARCHAR(10)  NOT NULL,
    device_id    VARCHAR(100),
    device_name  VARCHAR(120),
    app_version  VARCHAR(20),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_device_tokens_token UNIQUE (token)
);
CREATE INDEX idx_device_tokens_user ON device_tokens (user_id);

-- --- Sessions longues, rotatives et révocables --------------------------------
-- On ne stocke que le SHA-256 du jeton : une fuite de la base ne donne aucune
-- session utilisable, exactement comme pour un mot de passe.
--
-- family_id relie toutes les rotations successives d'une même connexion. Si un
-- jeton déjà consommé est represente, c'est qu'il a été volé (le client légitime,
-- lui, détient le dernier de la chaîne) : on révoque alors toute la famille plutôt
-- que la seule ligne rejouée.
CREATE TABLE refresh_tokens (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash   VARCHAR(64) NOT NULL,
    family_id    UUID        NOT NULL,
    platform     VARCHAR(10) NOT NULL DEFAULT 'WEB',
    device_id    VARCHAR(100),
    device_name  VARCHAR(120),
    expires_at   TIMESTAMP   NOT NULL,
    revoked_at   TIMESTAMP,
    used_at      TIMESTAMP,
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);
