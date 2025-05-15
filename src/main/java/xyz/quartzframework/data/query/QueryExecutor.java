package xyz.quartzframework.data.query;

import java.util.List;

public interface QueryExecutor<E> {

    List<E> execute(DynamicQueryDefinition query, Object[] args);

}