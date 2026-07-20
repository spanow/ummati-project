-- Coordonnées géographiques du bureau de l'organisation (mirror H2 de la migration Postgres V13).
ALTER TABLE organizations ADD COLUMN address_lat DECIMAL(10, 8);
ALTER TABLE organizations ADD COLUMN address_lng DECIMAL(11, 8);
