-- V7 : Seed data (H2 compatible)

-- Skills
INSERT INTO skills (name, category) VALUES ('Développement web', 'TECHNIQUE');
INSERT INTO skills (name, category) VALUES ('Graphisme', 'TECHNIQUE');
INSERT INTO skills (name, category) VALUES ('Cuisine collective', 'CUISINE');
INSERT INTO skills (name, category) VALUES ('Premiers secours', 'MEDICAL');
INSERT INTO skills (name, category) VALUES ('Transport', 'LOGISTIQUE');

-- PLATFORM_ADMIN (password: Admin123!)
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

