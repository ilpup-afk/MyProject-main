package com.example.demo.specifications;
import org.springframework.data.jpa.domain.Specification;
import com.example.demo.model.Bus;

public class BusSpecification {
    private static Specification<Bus> modelLike(String model) {
        return (root, query, criteriaBuilder) -> {
            if (model == null || model.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // всегда true
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("model")), 
                "%" + model.toLowerCase().trim() + "%"
            );
        };
    }
     private static Specification<Bus> numericFieldBetween(String fieldName, Integer min, Integer max) {
        return (root, query, criteriaBuilder) -> {
            if (min == null && max == null) {
                return criteriaBuilder.conjunction(); // всегда true
            } else if (min != null && max != null) {
                return criteriaBuilder.between(root.get(fieldName), min, max);
            } else if (min != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get(fieldName), min);
            } else {
                return criteriaBuilder.lessThanOrEqualTo(root.get(fieldName), max);
            }
        };
    }
     public static Specification<Bus> filter(String model, Integer min, Integer max) {
        return Specification.allOf(modelLike(model))
                           .and(numericFieldBetween("id", min, max));
    }
     public static Specification<Bus> filterByModel(String model) {
        return modelLike(model);
    }
}