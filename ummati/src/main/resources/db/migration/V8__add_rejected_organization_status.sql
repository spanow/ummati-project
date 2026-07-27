-- V8 : ajout du statut REJECTED aux organisations.
--
-- V3 crée la colonne avec une contrainte CHECK anonyme, que Postgres nomme
-- automatiquement « organizations_status_check ». Ajouter une contrainte du même nom
-- sans supprimer la précédente échouait sur toute base créée depuis zéro
-- (« constraint already exists ») : la migration ne passait que sur les bases où V3 et
-- V8 avaient été appliquées séparément à la main. On supprime donc explicitement avant
-- de recréer.
ALTER TABLE organizations DROP CONSTRAINT IF EXISTS organizations_status_check;

ALTER TABLE organizations ADD CONSTRAINT organizations_status_check
    CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'SUSPENDED', 'ARCHIVED'));
