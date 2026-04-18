-- V1 : Table users
CREATE TABLE users (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email            VARCHAR(255) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    phone            VARCHAR(20),
    date_of_birth    DATE,
    photo_url        VARCHAR(500),
    bio              TEXT,
    address_street   VARCHAR(255),
    address_city     VARCHAR(100),
    address_zip      VARCHAR(10),
    address_country  VARCHAR(100) DEFAULT 'France',
    role             VARCHAR(20)  NOT NULL DEFAULT 'VOLUNTEER'
                     CHECK (role IN ('VOLUNTEER', 'PLATFORM_ADMIN')),
    email_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    phone_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    locked_until     TIMESTAMP,
    failed_attempts  INT          NOT NULL DEFAULT 0,
    last_login_at    TIMESTAMP,
    onboarding_done  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);

