package xyz.quartzframework.data;

import lombok.RequiredArgsConstructor;
import lombok.val;
import xyz.quartzframework.core.bean.annotation.Priority;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.bean.factory.PluginBeanFactory;
import xyz.quartzframework.core.condition.annotation.ActivateWhenBeanMissing;
import xyz.quartzframework.core.context.AbstractQuartzContext;
import xyz.quartzframework.core.context.annotation.Configurer;
import xyz.quartzframework.data.annotation.DiscoverEntities;
import xyz.quartzframework.data.annotation.DiscoverStorages;
import xyz.quartzframework.data.entity.EntityDiscovery;
import xyz.quartzframework.data.entity.EntityRegistrar;
import xyz.quartzframework.data.query.CompositeQueryParser;
import xyz.quartzframework.data.query.MethodQueryParser;
import xyz.quartzframework.data.query.QQLQueryParser;
import xyz.quartzframework.data.storage.*;

import javax.annotation.PostConstruct;
import java.net.URLClassLoader;

@Configurer(force = true)
@RequiredArgsConstructor
public class QuartzDataConfigurer {

    private final AbstractQuartzContext<?> context;

    private final PluginBeanFactory pluginBeanFactory;

    private final CompositeQueryParser compositeQueryParser;

    @PostConstruct
    public void onConstruct() {
        compositeQueryParser.register(new QQLQueryParser());
        compositeQueryParser.register(new MethodQueryParser());
    }

    @Provide
    @Priority(1)
    @ActivateWhenBeanMissing(EntityDiscovery.class)
    EntityDiscovery entityDiscovery() {
        val discoverers = pluginBeanFactory.getBeansWithAnnotation(DiscoverEntities.class);
        return EntityDiscovery
                .builder(context)
                .addDiscoverers(discoverers);
    }

    @Provide
    @Priority(2)
    @ActivateWhenBeanMissing(EntityRegistrar.class)
    EntityRegistrar entityRegistrar(EntityDiscovery entityDiscovery) {
        return new EntityRegistrar(entityDiscovery);
    }

    @Provide
    @Priority(3)
    @ActivateWhenBeanMissing(StorageFactory.class)
    StorageFactory storageFactory(CompositeQueryParser queryParser, URLClassLoader classLoader) {
        return new DefaultStorageFactory(queryParser, classLoader, pluginBeanFactory);
    }

    @Provide
    @Priority(4)
    @ActivateWhenBeanMissing(StorageDiscovery.class)
    StorageDiscovery storageDiscovery() {
        val discoverers = pluginBeanFactory.getBeansWithAnnotation(DiscoverStorages.class);
        return StorageDiscovery
                .builder(context)
                .addDiscoverers(discoverers);
    }

    @Provide
    @Priority(5)
    @ActivateWhenBeanMissing(StorageRegistrar.class)
    StorageRegistrar storageRegistrar(StorageFactory storageFactory, StorageDiscovery storageDiscovery) {
        return new StorageRegistrar(context, storageDiscovery, storageFactory);
    }

    @Provide
    InMemoryStorageProvider inMemoryStorageProvider() {
        return new InMemoryStorageProvider();
    }

}