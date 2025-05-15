package xyz.quartzframework.data.storage;

import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.data.query.InMemoryQueryExecutor;
import xyz.quartzframework.data.query.QueryExecutor;

@Injectable
public class InMemoryStorageProvider<E, ID> implements StorageProvider<E, ID> {

    @Override
    public HashMapStorage<E, ID> create(Class<E> entity, Class<ID> id) {
        return new HashMapStorage<>(id);
    }

    @Override
    public QueryExecutor<E> getQueryExecutor(SimpleStorage<E, ID> storage) {
        return new InMemoryQueryExecutor<>(storage.findAll());
    }
}