package xyz.quartzframework.data.query;

import org.springframework.lang.Nullable;

import java.util.List;

public record DynamicQueryDefinition(
    QueryAction action,
    List<Condition> conditions,
    List<Order> orders,
    @Nullable Integer limit
) {}