package orga.takwa.ummati.repository;

import jakarta.persistence.criteria.*;
import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.Skill;
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
            predicates.add(cb.greaterThan(root.get("startDate"), now));

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
            if (startAfter != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startAfter));
            }
            if (startBefore != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), startBefore));
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

