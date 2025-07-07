package xyz.quartzframework.data.query;

import org.springframework.lang.Nullable;

public record Condition(
    String property,
    Operation operation,
    @Nullable Object fixedValue,
    @Nullable Integer paramIndex,
    @Nullable String namedParameter,
    boolean ignoreCase
) {

    public Condition(String property, Operation operation, Object fixedValue, boolean ignoreCase) {
        this(property, operation, fixedValue, null, null, ignoreCase);
    }

    public Condition(String property, Operation operation, Object fixedValue, Integer paramIndex, boolean ignoreCase) {
        this(property, operation, fixedValue, paramIndex, null, ignoreCase);
    }
}