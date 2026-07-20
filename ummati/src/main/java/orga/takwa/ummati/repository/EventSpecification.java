package orga.takwa.ummati.repository;

import jakarta.persistence.criteria.*;
import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.EventOccurrence;
import orga.takwa.ummati.entity.Skill;
import orga.takwa.ummati.entity.enums.EventOccurrenceStatus;
import orga.takwa.ummati.entity.enums.EventStatus;
import orga.takwa.ummati.entity.enums.EventType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EventSpecification {

    private EventSpecification() {}

    public static Specification<Event> search(EventStatus status, LocalDateTime now,
                                               EventType type, String city, UUID orgId,
                                               Boolean online, LocalDateTime startAfter,
                                               LocalDateTime startBefore, UUID skillId) {
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
            if (startAfter != null) {
                occPreds.add(cb.greaterThanOrEqualTo(occ.get("startDate"), startAfter));
            }
            if (startBefore != null) {
                occPreds.add(cb.lessThanOrEqualTo(occ.get("startDate"), startBefore));
            }
            sub.select(occ.get("id")).where(cb.and(occPreds.toArray(new Predicate[0])));
            predicates.add(cb.exists(sub));

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (city != null) {
                predicates.add(cb.equal(cb.lower(root.get("locationCity")), city.toLowerCase()));
            }
            if (orgId != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), orgId));
            }
            if (online != null) {
                predicates.add(cb.equal(root.get("online"), online));
            }
            if (skillId != null) {
                Join<Event, Skill> skills = root.join("requiredSkills", JoinType.LEFT);
                predicates.add(cb.equal(skills.get("id"), skillId));
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
