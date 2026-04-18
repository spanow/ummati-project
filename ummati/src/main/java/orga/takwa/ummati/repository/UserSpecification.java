package orga.takwa.ummati.repository;

import jakarta.persistence.criteria.Predicate;
import orga.takwa.ummati.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> search(String searchText, Boolean enabled) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchText != null) {
                String pattern = "%" + searchText.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
                ));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }

            return predicates.isEmpty()
                ? cb.conjunction()
                : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

