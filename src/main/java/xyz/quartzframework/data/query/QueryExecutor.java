package xyz.quartzframework.data.query;

import xyz.quartzframework.data.page.Page;
import xyz.quartzframework.data.page.Pagination;

import java.util.List;

public interface QueryExecutor<E> {

    List<E> find(DynamicQueryDefinition query, Object[] args);

    Page<E> find(DynamicQueryDefinition query, Object[] args, Pagination pagination);

    long count(DynamicQueryDefinition query, Object[] args);

    boolean exists(DynamicQueryDefinition query, Object[] args);
}