package com.tivit.snap_api.spec;

import com.tivit.snap_api.core.SnapResourceMeta;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class SnapSpecBuilder {

    public static <T> Specification<T> build(SnapResourceMeta meta, Map<String, String> params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.equals("page") || key.equals("size") || key.equals("sort")) {
                    continue;
                }

                if (key.contains("_")) {
                    String[] parts = key.split("_", 2);
                    String fieldName = parts[0];
                    String operator = parts[1].toLowerCase();

                    if (meta.searchableFields().contains(fieldName)) {
                        predicates.add(createPredicate(root, cb, fieldName, operator, value));
                    }
                }

                else if (meta.searchableFields().contains(key)) {
                    Path<?> path = getPath(root, key);
                    Class<?> fieldType = path.getJavaType();

                    if (String.class.isAssignableFrom(fieldType)) {
                        predicates.add(cb.like(cb.lower((Path<String>) path),
                                "%" + value.toLowerCase() + "%"));
                    } else {
                        predicates.add(createEqualsPredicate(cb, path, value, fieldType));
                    }
                }
            }

            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static <T> Predicate createPredicate(Root<T> root, jakarta.persistence.criteria.CriteriaBuilder cb,
                                                 String fieldName, String operator, String value) {
        Path<?> path = getPath(root, fieldName);
        Class<?> fieldType = path.getJavaType();

        return switch (operator) {
            case "neq" -> cb.not(createEqualsPredicate(cb, path, value, fieldType));
            case "gt" -> createComparisonPredicate(cb, path, value, fieldType, "gt");
            case "lt" -> createComparisonPredicate(cb, path, value, fieldType, "lt");
            case "gte" -> createComparisonPredicate(cb, path, value, fieldType, "gte");
            case "lte" -> createComparisonPredicate(cb, path, value, fieldType, "lte");
            case "like" -> {
                if (String.class.isAssignableFrom(fieldType)) {
                    yield cb.like(cb.lower((Path<String>) path), "%" + value.toLowerCase() + "%");
                } else {
                    yield createEqualsPredicate(cb, path, value, fieldType);
                }
            }
            case "isnull" -> value.equalsIgnoreCase("true")
                    ? cb.isNull(path)
                    : cb.isNotNull(path);
            default -> createEqualsPredicate(cb, path, value, fieldType);
        };
    }

    private static Predicate createEqualsPredicate(jakarta.persistence.criteria.CriteriaBuilder cb,
                                                   Path<?> path, String value, Class<?> fieldType) {
        Object convertedValue = convertValue(value, fieldType);
        if (convertedValue == null) {
            return cb.isNull(path);
        }
        return cb.equal(path, convertedValue);
    }

    private static Predicate createComparisonPredicate(jakarta.persistence.criteria.CriteriaBuilder cb,
                                                       Path<?> path, String value, Class<?> fieldType,
                                                       String operator) {
        Object convertedValue = convertValue(value, fieldType);

        if (convertedValue instanceof Number number) {
            if (number instanceof Integer || number instanceof Long) {
                long longValue = number.longValue();
                return switch (operator) {
                    case "gt" -> cb.greaterThan((Path<Long>) path, longValue);
                    case "lt" -> cb.lessThan((Path<Long>) path, longValue);
                    case "gte" -> cb.greaterThanOrEqualTo((Path<Long>) path, longValue);
                    case "lte" -> cb.lessThanOrEqualTo((Path<Long>) path, longValue);
                    default -> cb.equal(path, number);
                };
            } else {
                double doubleValue = number.doubleValue();
                return switch (operator) {
                    case "gt" -> cb.greaterThan((Path<Double>) path, doubleValue);
                    case "lt" -> cb.lessThan((Path<Double>) path, doubleValue);
                    case "gte" -> cb.greaterThanOrEqualTo((Path<Double>) path, doubleValue);
                    case "lte" -> cb.lessThanOrEqualTo((Path<Double>) path, doubleValue);
                    default -> cb.equal(path, number);
                };
            }
        } else if (convertedValue instanceof LocalDate date) {
            return switch (operator) {
                case "gt" -> cb.greaterThan((Path<LocalDate>) path, date);
                case "lt" -> cb.lessThan((Path<LocalDate>) path, date);
                case "gte" -> cb.greaterThanOrEqualTo((Path<LocalDate>) path, date);
                case "lte" -> cb.lessThanOrEqualTo((Path<LocalDate>) path, date);
                default -> cb.equal(path, date);
            };
        } else if (convertedValue instanceof LocalDateTime dateTime) {
            return switch (operator) {
                case "gt" -> cb.greaterThan((Path<LocalDateTime>) path, dateTime);
                case "lt" -> cb.lessThan((Path<LocalDateTime>) path, dateTime);
                case "gte" -> cb.greaterThanOrEqualTo((Path<LocalDateTime>) path, dateTime);
                case "lte" -> cb.lessThanOrEqualTo((Path<LocalDateTime>) path, dateTime);
                default -> cb.equal(path, dateTime);
            };
        }

        return cb.equal(path, convertedValue);
    }

    private static Object convertValue(String value, Class<?> targetType) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(value);
            } else if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(value);
            } else if (targetType == Double.class || targetType == double.class) {
                return Double.parseDouble(value);
            } else if (targetType == Float.class || targetType == float.class) {
                return Float.parseFloat(value);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (targetType == BigDecimal.class) {
                return new BigDecimal(value);
            } else if (targetType == LocalDate.class) {
                return LocalDate.parse(value);
            } else if (targetType == LocalDateTime.class) {
                try {
                    return LocalDateTime.parse(value);
                } catch (Exception e) {
                    return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
            }
        } catch (Exception e) {
            return null;
        }

        return value;
    }

    private static Path<?> getPath(Root<?> root, String fieldPath) {
        Path<?> path = root;
        for (String part : fieldPath.split("\\.")) {
            path = path.get(part);
        }
        return path;
    }
}