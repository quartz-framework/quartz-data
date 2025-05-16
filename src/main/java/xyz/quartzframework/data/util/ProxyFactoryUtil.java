package xyz.quartzframework.data.util;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.springframework.aop.framework.ProxyFactory;
import xyz.quartzframework.data.query.QueryExecutor;
import xyz.quartzframework.data.query.QueryParser;
import xyz.quartzframework.data.storage.SimpleStorage;
import xyz.quartzframework.data.storage.StorageMethodInterceptor;

@UtilityClass
public class ProxyFactoryUtil {

    public <E, S> S createProxy(QueryParser queryParser, Class<S> storageInterface, QueryExecutor<E> executor, Class<E> entityType) {
        ProxyFactory factory = new ProxyFactory();
        factory.setInterfaces(storageInterface, SimpleStorage.class);
        factory.addAdvice(new StorageMethodInterceptor<>(queryParser, executor, entityType));
        return (S) factory.getProxy();
    }

    public <E, ID> ProxyFactory createProxyFactory(QueryParser queryParser,
                                                   SimpleStorage<E, ID> target,
                                                   Class<E> entityType,
                                                   Class<? extends SimpleStorage<E, ID>> storageInterface,
                                                   QueryExecutor<E> executor) {
        val proxyFactory = new ProxyFactory();
        proxyFactory.setInterfaces(storageInterface);
        proxyFactory.setTarget(target);
        proxyFactory.addAdvice(new StorageMethodInterceptor<>(queryParser, executor, entityType));
        return proxyFactory;
    }
}