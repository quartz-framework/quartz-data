package xyz.quartzframework.data.storage;

import lombok.RequiredArgsConstructor;
import lombok.val;
import xyz.quartzframework.core.bean.factory.PluginBeanFactory;
import xyz.quartzframework.data.annotation.Storage;
import xyz.quartzframework.data.annotation.SuperStorage;
import xyz.quartzframework.data.util.GenericTypeUtil;
import xyz.quartzframework.data.util.ProxyFactoryUtil;

import java.net.URLClassLoader;
import java.util.Arrays;

@RequiredArgsConstructor
public class DefaultStorageFactory implements StorageFactory {

    private final URLClassLoader classLoader;

    private final PluginBeanFactory beanFactory;

    @Override
    @SuppressWarnings("unchecked")
    public <E, ID> SimpleStorage<E, ID> create(Class<? extends SimpleStorage<E, ID>> storageInterface) {
        if (!storageInterface.isAnnotationPresent(Storage.class)) {
            throw new IllegalArgumentException(storageInterface.getName() + " is not annotated with @Storage");
        }
        Class<?> superInterface = Arrays.stream(storageInterface.getInterfaces())
                .filter(i -> i.isAnnotationPresent(SuperStorage.class))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No @SuperStorage found in " + storageInterface.getName()));
        val annotation = superInterface.getAnnotation(SuperStorage.class);
        if (annotation == null) {
            throw new IllegalArgumentException(storageInterface.getName() + " is not annotated with @SuperStorage");
        }
        Class<?> implClass = annotation.value();
        Class<?>[] types = GenericTypeUtil.resolve(storageInterface, SimpleStorage.class);
        if (types == null || types.length != 2) {
            throw new IllegalArgumentException(storageInterface.getName() + " is not a supported storage interface");
        }
        Class<E> entityType = (Class<E>) types[0];
        Class<ID> idType = (Class<ID>) types[1];
        Object bean = beanFactory.getBean(implClass);
        if (bean instanceof StorageProvider<?, ?> provider) {
            val p = (StorageProvider<E, ID>) provider;
            val target = p.create(entityType, idType);
            val proxyFactory = ProxyFactoryUtil.createProxyFactory(target, entityType, storageInterface, p.getQueryExecutor(target));
            return (SimpleStorage<E, ID>) proxyFactory.getProxy(classLoader);
        }
        throw new IllegalStateException("Provided class " + implClass.getName() + " is not a StorageProvider");
    }
}