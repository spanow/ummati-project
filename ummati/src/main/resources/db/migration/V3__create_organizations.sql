-- V3 : Table organizations
CREATE TABLE organizations (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(255) NOT NULL UNIQUE,
    slug             VARCHAR(255) NOT NULL UNIQUE,
    description      TEXT         NOT NULL,
    mission          TEXT,
    domain           VARCHAR(30)  NOT NULL
                     CHECK (domain IN ('EDUCATION','SANTE','ENVIRONNEMENT','SOCIAL',
                                       'CULTURE','SPORT','HUMANITAIRE','DROITS_HUMAINS',
                                       'AIDE_URGENCE','AUTRE')),
    logo_url         VARCHAR(500),
    banner_url       VARCHAR(500),
    address_street   VARCHAR(255),
    address_city     VARCHAR(100) NOT NULL,
    address_zip      VARCHAR(10)  NOT NULL,
    address_country  VARCHAR(100) DEFAULT 'France',
    phone            VARCHAR(20),
    email            VARCHAR(255) NOT NULL,
    website          VARCHAR(500),
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','ACTIVE','SUSPENDED','ARCHIVED')),
    rejection_reason TEXT,
    created_by       UUID         NOT NULL REFERENCES users(id),
    validated_by     UUID         REFERENCES users(id),
    validated_at     TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orgs_slug ON organizations (slug);
CREATE INDEX idx_orgs_status ON organizations (status);
CREATE INDEX idx_orgs_domain ON organizations (domain);
CREATE INDEX idx_orgs_city ON organizations (address_city);

