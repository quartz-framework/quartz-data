package xyz.quartzframework.data.query;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings("unchecked")
public class InMemoryQueryExecutor<E> implements QueryExecutor<E> {

    private final Collection<E> source;

    public InMemoryQueryExecutor(Collection<E> source) {
        this.source = List.copyOf(source);
    }

    @Override
    public List<E> execute(DynamicQueryDefinition query, Object[] args) {
        log.info("Executing query: {}", query);
        List<E> result = new ArrayList<>(source);
        int argIndex = 0;

        for (Condition condition : query.conditions()) {
            Object value;
            if (condition.fixedValue() != null || condition.operation() == Operation.IS_NULL || condition.operation() == Operation.IS_NOT_NULL) {
                value = condition.fixedValue();
            } else if (condition.paramIndex() != null) {
                value = args[condition.paramIndex()];
            } else {
                value = args[argIndex++];
            }

            log.info("Applying condition: {} {} {}", condition.property(), condition.operation(), value);

            Object finalValue = value;
            result = result.stream().filter(entity -> {
                try {
                    Object fieldValue = getNestedFieldValue(entity, condition.property());
                    boolean matched = match(fieldValue, condition.operation(), finalValue);
                    log.info(" -> Entity: {}, Field '{}': {}, Match: {}", entity, condition.property(), fieldValue, matched);
                    return matched;
                } catch (Exception e) {
                    log.warn("Failed to evaluate condition on entity: {}", entity, e);
                    return false;
                }
            }).collect(Collectors.toList());
        }

        if (!query.orders().isEmpty()) {
            result.sort((a, b) -> {
                for (Order order : query.orders()) {
                    try {
                        Object va = getNestedFieldValue(a, order.property());
                        Object vb = getNestedFieldValue(b, order.property());
                        if (va == null && vb == null) continue;
                        if (va == null) return order.descending() ? 1 : -1;
                        if (vb == null) return order.descending() ? -1 : 1;
                        if (va instanceof Comparable<?> && va.getClass().equals(vb.getClass())) {
                            @SuppressWarnings("unchecked")
                            Comparable<Object> cmpA = (Comparable<Object>) va;
                            int cmp = cmpA.compareTo(vb);
                            if (cmp != 0) return order.descending() ? -cmp : cmp;
                        }
                    } catch (Exception e) {
                        log.warn("Ordering failed for properties: {}", order.property(), e);
                    }
                }
                return 0;
            });
        }

        if (query.limit() != null && query.limit() > 0 && result.size() > query.limit()) {
            result = result.subList(0, query.limit());
        }
        return result;
    }

    private boolean match(Object fieldValue, Operation operation, Object expectedValue) {
        log.info("Matching: fieldValue = {} ({}) | expected = {} ({})",
                fieldValue, fieldValue != null ? fieldValue.getClass() : "null",
                expectedValue, expectedValue != null ? expectedValue.getClass() : "null");
        try {
            if (operation == Operation.EQUAL) return Objects.equals(fieldValue, expectedValue);
            if (operation == Operation.NOT_EQUAL) return !Objects.equals(fieldValue, expectedValue);
            if (operation == Operation.GREATER_THAN && fieldValue instanceof Comparable) {
                assert expectedValue != null;
                return ((Comparable<Object>) fieldValue).compareTo(expectedValue) > 0;
            }
            if (operation == Operation.GREATER_THAN_OR_EQUAL && fieldValue instanceof Comparable && expectedValue != null) {
                return ((Comparable<Object>) fieldValue).compareTo(expectedValue) >= 0;
            }
            if (operation == Operation.LESS_THAN && fieldValue instanceof Comparable) {
                assert expectedValue != null;
                return ((Comparable<Object>) fieldValue).compareTo(expectedValue) < 0;
            }
            if (operation == Operation.LESS_THAN_OR_EQUAL && fieldValue instanceof Comparable && expectedValue != null) {
                return ((Comparable<Object>) fieldValue).compareTo(expectedValue) <= 0;
            }
            if ((operation == Operation.LIKE || operation == Operation.NOT_LIKE)
                    && fieldValue instanceof String str
                    && expectedValue instanceof String pattern) {
                boolean matches;
                if (!pattern.contains("%") && !pattern.contains("_")) {
                    matches = str.contains(pattern);
                } else {
                    String regex = likeToRegex(pattern);
                    matches = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(str).matches();
                }
                return (operation == Operation.LIKE) == matches;
            }
            if (operation == Operation.IS_NULL) return fieldValue == null;
            if (operation == Operation.IS_NOT_NULL) return fieldValue != null;
            if (operation == Operation.IN && expectedValue instanceof Collection<?> collection)
                return collection.contains(fieldValue);
            if (operation == Operation.NOT_IN && expectedValue instanceof Collection<?> collection)
                return !collection.contains(fieldValue);
        } catch (Exception e) {
            log.warn("Failed to match: fieldValue={}, operation={}, expectedValue={}", fieldValue, operation, expectedValue, e);
        }

        return false;
    }

    private Object getNestedFieldValue(Object root, String path) throws Exception {
        String[] parts = path.split("\\.");
        Object current = root;
        for (String part : parts) {
            if (current == null) return null;
            Field field = findField(current.getClass(), part);
            field.setAccessible(true);
            current = field.get(current);
        }
        return current;
    }

    private Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field '" + name + "' not found");
    }

    private String likeToRegex(String pattern) {
        StringBuilder regex = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            switch (c) {
                case '%': regex.append(".*"); break;
                case '_': regex.append('.'); break;
                case '\\': regex.append("\\\\"); break;
                case '^', '$', '.', '|', '?', '*', '+', '(', ')', '[', '{':
                    regex.append('\\').append(c); break;
                default: regex.append(c);
            }
        }
        regex.insert(0, "^");
        regex.append("$");
        return regex.toString();
    }
}