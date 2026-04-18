-- V7 : Seed skills & platform admin

-- TECHNIQUE
INSERT INTO skills (name, category) VALUES
    ('Développement web', 'TECHNIQUE'),
    ('Graphisme', 'TECHNIQUE'),
    ('Photographie', 'TECHNIQUE'),
    ('Vidéo', 'TECHNIQUE');

-- COMMUNICATION
INSERT INTO skills (name, category) VALUES
    ('Rédaction', 'COMMUNICATION'),
    ('Réseaux sociaux', 'COMMUNICATION'),
    ('Relations presse', 'COMMUNICATION'),
    ('Traduction', 'COMMUNICATION');

-- LOGISTIQUE
INSERT INTO skills (name, category) VALUES
    ('Transport', 'LOGISTIQUE'),
    ('Manutention', 'LOGISTIQUE'),
    ('Organisation événementielle', 'LOGISTIQUE');

-- MEDICAL
INSERT INTO skills (name, category) VALUES
    ('Premiers secours', 'MEDICAL'),
    ('Infirmerie', 'MEDICAL'),
    ('Psychologie', 'MEDICAL');

-- JURIDIQUE
INSERT INTO skills (name, category) VALUES
    ('Droit associatif', 'JURIDIQUE'),
    ('Conseil juridique', 'JURIDIQUE');

-- ENSEIGNEMENT
INSERT INTO skills (name, category) VALUES
    ('Soutien scolaire', 'ENSEIGNEMENT'),
    ('Formation adultes', 'ENSEIGNEMENT'),
    ('Alphabétisation', 'ENSEIGNEMENT');

-- CUISINE
INSERT INTO skills (name, category) VALUES
    ('Cuisine collective', 'CUISINE'),
    ('Pâtisserie', 'CUISINE');

-- CONDUITE
INSERT INTO skills (name, category) VALUES
    ('Permis B', 'CONDUITE'),
    ('Permis C', 'CONDUITE');

-- LANGUE
INSERT INTO skills (name, category) VALUES
    ('Français', 'LANGUE'),
    ('Anglais', 'LANGUE'),
    ('Arabe', 'LANGUE'),
    ('Espagnol', 'LANGUE');

-- PLATFORM_ADMIN seed (password: Admin123! — bcrypt cost 12)
-- This hash must be regenerated in production
INSERT INTO users (email, password_hash, first_name, last_name, role, email_verified, enabled, onboarding_done)
VALUES (
    'admin@ummati.org',
    '$2a$12$LJ3m4ys3uz2YHiKfMbBpO.KRqmBEePN0BZqXMZig4RMVFJOjRABPG',
    'Admin',
    'Ummati',
    'PLATFORM_ADMIN',
    TRUE,
    TRUE,
    TRUE
);

