package xyz.quartzframework.data.storage;

public record StorageDefinition(
        Class<?> storageInterface,
        Class<?> entityClass,
        Class<?> idClass
) { }