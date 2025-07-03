package xyz.quartzframework.data.query;

import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.List;

public record DynamicQueryDefinition(
    Method method,
    QueryAction action,
    List<Condition> conditions,
    List<Order> orders,
    @Nullable Integer limit,
    boolean distinct,
    boolean nativeSQL,
    @Nullable String raw
) {}