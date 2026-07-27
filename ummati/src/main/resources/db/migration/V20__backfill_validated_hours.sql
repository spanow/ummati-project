-- V20 : reprise des heures de bénévolat des présences antérieures à leur certification.
--
-- Depuis la fusion des heures validées (V15), toute validation de présence écrit
-- hours_validated. Les présences enregistrées avant cette bascule sont restées à NULL et
-- seraient comptées zéro heure dans le profil, le passeport et l'attestation.
--
-- On les renseigne une bonne fois à partir de la durée du créneau réservé — c'est la
-- meilleure approximation disponible — puis la valeur est figée comme les autres :
-- plus aucun recalcul à partir des dates par la suite.

UPDATE event_signups s
SET hours_validated = ROUND(
        GREATEST(EXTRACT(EPOCH FROM (o.end_date - o.start_date)) / 3600.0, 0)::numeric, 2)
FROM event_occurrences o
WHERE s.occurrence_id = o.id
  AND s.status = 'ATTENDED'
  AND s.hours_validated IS NULL;

-- Filet : une présence sans créneau rattaché (données héritées) reste à zéro plutôt
-- qu'à NULL, pour que la somme des heures soit toujours définie.
UPDATE event_signups
SET hours_validated = 0
WHERE status = 'ATTENDED'
  AND hours_validated IS NULL;
