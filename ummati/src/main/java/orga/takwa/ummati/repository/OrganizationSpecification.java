package orga.takwa.ummati.repository;

import jakarta.persistence.criteria.Predicate;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.enums.OrganizationDomain;
import orga.takwa.ummati.entity.enums.OrganizationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class OrganizationSpecification {

    private OrganizationSpecification() {}

    public static Specification<Organization> search(OrganizationStatus status,
                                                      OrganizationDomain domain,
                                                      String city,
                                                      String searchText) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), status));

            if (domain != null) {
                predicates.add(cb.equal(root.get("domain"), domain));
            }
            if (city != null) {
                predicates.add(cb.equal(cb.lower(root.get("addressCity")), city.toLowerCase()));
            }
            if (searchText != null) {
                String pattern = "%" + searchText.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

