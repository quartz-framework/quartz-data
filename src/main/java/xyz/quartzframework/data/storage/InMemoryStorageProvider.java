package xyz.quartzframework.data.storage;

import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.data.query.InMemoryQueryExecutor;
import xyz.quartzframework.data.query.QueryExecutor;

@Injectable
public class InMemoryStorageProvider implements StorageProvider {

    @Override
    public <E, ID> HashMapStorage<E, ID> create(Class<E> entity, Class<ID> id) {
        return new HashMapStorage<>(id);
    }

    @Override
    public <E, ID> QueryExecutor<E> getQueryExecutor(SimpleStorage<E, ID> storage) {
        return new InMemoryQueryExecutor<>(storage.findAll());
    }
}