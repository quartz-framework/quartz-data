package xyz.quartzframework.data.query;

import org.springframework.lang.Nullable;

public record Condition(
    String property,
    Operation operation,
    @Nullable Object fixedValue,
    @Nullable Integer paramIndex
) {

    public Condition(String property, Operation operation, Object fixedValue) {
        this(property, operation, fixedValue, null);
    }
}