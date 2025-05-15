package xyz.quartzframework.data;

import lombok.RequiredArgsConstructor;
import lombok.val;
import xyz.quartzframework.core.bean.annotation.Priority;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.bean.factory.PluginBeanFactory;
import xyz.quartzframework.core.bean.registry.PluginBeanDefinitionRegistry;
import xyz.quartzframework.core.condition.annotation.ActivateWhenBeanMissing;
import xyz.quartzframework.core.context.AbstractQuartzContext;
import xyz.quartzframework.core.context.annotation.Configurer;
import xyz.quartzframework.data.annotation.DiscoverStorages;
import xyz.quartzframework.data.storage.DefaultStorageFactory;
import xyz.quartzframework.data.storage.StorageDiscovery;
import xyz.quartzframework.data.storage.StorageFactory;
import xyz.quartzframework.data.storage.StorageRegistrar;

import java.net.URLClassLoader;

@Configurer(force = true)
@RequiredArgsConstructor
public class QuartzDataConfigurer {

    private final AbstractQuartzContext<?> context;

    private final PluginBeanFactory pluginBeanFactory;

    private final PluginBeanDefinitionRegistry pluginBeanDefinitionRegistry;

    @Provide
    @Priority(0)
    @ActivateWhenBeanMissing(StorageFactory.class)
    StorageFactory storageFactory(URLClassLoader classLoader) {
        return new DefaultStorageFactory(classLoader, pluginBeanFactory);
    }

    @Provide
    @Priority(1)
    @ActivateWhenBeanMissing(StorageDiscovery.class)
    StorageDiscovery storageDiscovery() {
        val discoverers = pluginBeanFactory.getBeansWithAnnotation(DiscoverStorages.class);
        return StorageDiscovery
                .builder(context)
                .addDiscoverers(discoverers);
    }

    @Provide
    @Priority(2)
    @ActivateWhenBeanMissing(StorageRegistrar.class)
    StorageRegistrar storageRegistrar(StorageFactory storageFactory, StorageDiscovery storageDiscovery) {
        return new StorageRegistrar(pluginBeanDefinitionRegistry, storageDiscovery, storageFactory);
    }
}