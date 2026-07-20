-- Coordonnées géographiques du bureau de l'organisation (géocodées via OpenStreetMap / Nominatim).
-- Mêmes précisions que events.location_lat / location_lng.
ALTER TABLE organizations ADD COLUMN address_lat DECIMAL(10, 8);
ALTER TABLE organizations ADD COLUMN address_lng DECIMAL(11, 8);
