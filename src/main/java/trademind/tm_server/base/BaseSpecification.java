package trademind.tm_server.base;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseSpecification<T> implements Specification<T> {

    private final Map<String, Object> filters;
    private final Map<String, String> relationFilters; // relationName -> id field

    public BaseSpecification(Map<String, Object> filters, Map<String, String> relationFilters) {
        this.filters = filters;
        this.relationFilters = relationFilters;
    }

    @Override
    public jakarta.persistence.criteria.Predicate toPredicate(jakarta.persistence.criteria.Root<T> root,
                                                              jakarta.persistence.criteria.CriteriaQuery<?> query,
                                                              jakarta.persistence.criteria.CriteriaBuilder cb) {

        List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

        // 1️⃣ Simple field filters
        if (filters != null) {
            filters.forEach((field, value) -> {
                if (value != null) {
                    if (value instanceof String) {
                        predicates.add(cb.like(cb.lower(root.get(field)), "%" + value.toString().toLowerCase() + "%"));
                    } else if (value instanceof java.util.Collection<?> values) {
                        // e.g. CapitalSearchRequest's `reasons` list -- lets a
                        // caller filter to a small set of enum values (DEPOSIT,
                        // WITHDRAW) in one query instead of one equality filter
                        // per value or pulling everything and filtering client-side.
                        if (!values.isEmpty()) {
                            predicates.add(root.get(field).in(values));
                        }
                    } else {
                        predicates.add(cb.equal(root.get(field), value));
                    }
                }
            });
        }

        // 2️⃣ Relation filters
        if (relationFilters != null) {
            relationFilters.forEach((relation, idFieldName) -> {
                var join = root.join(relation, JoinType.LEFT);
                Object idValue = filters.get(idFieldName);
                if (idValue != null) {
                    predicates.add(cb.equal(join.get("id"), idValue));
                    query.distinct(true); // avoid duplicates from many-to-many
                }
            });
        }

        if (predicates.isEmpty()) {
            return cb.conjunction();
        }

        return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
    }
}
