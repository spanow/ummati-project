package orga.takwa.ummati.repository;

import jakarta.persistence.criteria.*;
import orga.takwa.ummati.dto.event.EventSearchCriteria;
import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.EventOccurrence;
import orga.takwa.ummati.entity.Skill;
import orga.takwa.ummati.entity.enums.EventOccurrenceStatus;
import orga.takwa.ummati.entity.enums.EventStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EventSpecification {

    /** Rayon terrestre moyen, en kilomètres. */
    static final double EARTH_RADIUS_KM = 6371.0;

    private EventSpecification() {}

    public static Specification<Event> search(EventStatus status, LocalDateTime now, EventSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), status));

            // Il existe un créneau PUBLISHED à venir (+ filtres de dates) pour cet événement.
            // On filtre sur les occurrences, pas sur l'enveloppe : une série récurrente dont le
            // premier créneau est passé mais qui a des créneaux futurs reste listée.
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<EventOccurrence> occ = sub.from(EventOccurrence.class);
            List<Predicate> occPreds = new ArrayList<>();
            occPreds.add(cb.equal(occ.get("event").get("id"), root.get("id")));
            occPreds.add(cb.equal(occ.get("status"), EventOccurrenceStatus.PUBLISHED));
            occPreds.add(cb.greaterThan(occ.get("startDate"), now));
            if (c.startAfter() != null) {
                occPreds.add(cb.greaterThanOrEqualTo(occ.get("startDate"), c.startAfter()));
            }
            if (c.startBefore() != null) {
                occPreds.add(cb.lessThanOrEqualTo(occ.get("startDate"), c.startBefore()));
            }
            sub.select(occ.get("id")).where(cb.and(occPreds.toArray(new Predicate[0])));
            predicates.add(cb.exists(sub));

            if (c.type() != null) {
                predicates.add(cb.equal(root.get("type"), c.type()));
            }
            if (c.city() != null) {
                predicates.add(cb.equal(cb.lower(root.get("locationCity")), c.city().toLowerCase()));
            }
            if (c.orgId() != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), c.orgId()));
            }
            if (c.online() != null) {
                predicates.add(cb.equal(root.get("online"), c.online()));
            }
            if (c.skillId() != null) {
                Join<Event, Skill> skills = root.join("requiredSkills", JoinType.LEFT);
                predicates.add(cb.equal(skills.get("id"), c.skillId()));
                query.distinct(true);
            }

            // --- Recherche plein texte -----------------------------------------
            // Volontairement simple (LIKE insensible à la casse sur quelques colonnes) :
            // suffisant au volume actuel, à remplacer par un index tsvector Postgres —
            // ou Elasticsearch (T-301) — quand le catalogue grossira.
            if (c.q() != null && !c.q().isBlank()) {
                String pattern = "%" + c.q().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("objectives"), "")), pattern),
                        cb.like(cb.lower(root.get("locationCity")), pattern),
                        cb.like(cb.lower(root.get("organization").get("name")), pattern)
                ));
            }

            // --- Recherche géographique ----------------------------------------
            if (c.hasOrigin()) {
                Expression<Double> cosDistance = cosineOfAngularDistance(root, cb, c.lat(), c.lng());

                if (c.hasRadius()) {
                    // Un événement sans coordonnées ne peut pas être situé : il sort du périmètre.
                    predicates.add(cb.isNotNull(root.get("locationLat")));
                    predicates.add(cb.isNotNull(root.get("locationLng")));

                    // Pré-filtre par bounding box : simples comparaisons, exploitables par
                    // l'index idx_events_coordinates, qui écarte l'essentiel des lignes avant
                    // le calcul trigonométrique.
                    double latDelta = Math.toDegrees(c.radiusKm() / EARTH_RADIUS_KM);
                    double cosLat = Math.cos(Math.toRadians(c.lat()));
                    predicates.add(cb.between(root.get("locationLat").as(Double.class),
                            c.lat() - latDelta, c.lat() + latDelta));
                    // Près des pôles cos(lat) tend vers 0 : le filtre en longitude perd son sens,
                    // on le laisse alors tomber plutôt que de produire des bornes absurdes.
                    if (Math.abs(cosLat) > 1e-6) {
                        double lngDelta = latDelta / Math.abs(cosLat);
                        if (lngDelta < 180.0) {
                            predicates.add(cb.between(root.get("locationLng").as(Double.class),
                                    c.lng() - lngDelta, c.lng() + lngDelta));
                        }
                    }

                    // Filtre exact. On compare des cosinus plutôt que des distances : cos est
                    // décroissant sur [0, π], donc « distance <= rayon » équivaut à
                    // « cos(angle) >= cos(rayon/R) ». Cela évite acos(), dont l'argument peut
                    // déborder de [-1, 1] par erreur d'arrondi et faire échouer la requête.
                    predicates.add(cb.greaterThanOrEqualTo(cosDistance,
                            Math.cos(c.radiusKm() / EARTH_RADIUS_KM)));
                }

                if (c.sortByDistance()) {
                    // cos décroissant = distance croissante : le plus proche d'abord.
                    query.orderBy(cb.desc(cosDistance));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Cosinus de la distance angulaire entre le point de référence et l'événement.
     * Décroissant avec la distance réelle : utilisable tel quel pour filtrer et pour
     * trier, sans jamais appeler acos().
     */
    private static Expression<Double> cosineOfAngularDistance(Root<Event> root, CriteriaBuilder cb,
                                                              double lat, double lng) {
        Expression<Double> latRad = cb.function("radians", Double.class, root.get("locationLat").as(Double.class));
        Expression<Double> lngRad = cb.function("radians", Double.class, root.get("locationLng").as(Double.class));

        double originLatRad = Math.toRadians(lat);
        double originLngRad = Math.toRadians(lng);

        Expression<Double> cosTerm = cb.prod(
                cb.prod(
                        cb.literal(Math.cos(originLatRad)),
                        cb.function("cos", Double.class, latRad)),
                cb.function("cos", Double.class, cb.diff(lngRad, cb.literal(originLngRad))));

        Expression<Double> sinTerm = cb.prod(
                cb.literal(Math.sin(originLatRad)),
                cb.function("sin", Double.class, latRad));

        return cb.sum(cosTerm, sinTerm);
    }

    /**
     * Distance orthodromique en kilomètres, calculée côté application pour enrichir la
     * réponse renvoyée au client (le tri, lui, reste en base).
     */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
