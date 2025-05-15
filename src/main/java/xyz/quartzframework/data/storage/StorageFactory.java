package xyz.quartzframework.data.storage;

public interface StorageFactory {

    <E, ID> SimpleStorage<E, ID> create(Class<? extends SimpleStorage<E, ID>> storageInterface);

}