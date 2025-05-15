package xyz.quartzframework.data.util;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.springframework.aop.framework.ProxyFactory;
import xyz.quartzframework.data.query.QueryExecutor;
import xyz.quartzframework.data.storage.SimpleStorage;
import xyz.quartzframework.data.storage.StorageMethodInterceptor;

@UtilityClass
public class ProxyFactoryUtil {

    public <E, S> S createProxy(Class<S> storageInterface, QueryExecutor<E> executor, Class<E> entityType) {
        ProxyFactory factory = new ProxyFactory();
        factory.setInterfaces(storageInterface, SimpleStorage.class);
        factory.addAdvice(new StorageMethodInterceptor<>(executor, entityType));
        return (S) factory.getProxy();
    }

    public <E, ID> ProxyFactory createProxyFactory(SimpleStorage<E, ID> target,
                                                      Class<E> entityType,
                                                      Class<? extends SimpleStorage<E, ID>> storageInterface,
                                                      QueryExecutor<E> executor) {
        val proxyFactory = new ProxyFactory();
        proxyFactory.setInterfaces(storageInterface);
        proxyFactory.setTarget(target);
        proxyFactory.addAdvice(new StorageMethodInterceptor<>(executor, entityType));
        return proxyFactory;
    }
}