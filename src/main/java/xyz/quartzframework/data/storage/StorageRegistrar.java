package xyz.quartzframework.data.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import xyz.quartzframework.core.bean.registry.PluginBeanDefinitionRegistry;

@Slf4j
@RequiredArgsConstructor
public class StorageRegistrar {

    public StorageRegistrar(PluginBeanDefinitionRegistry registry,
                            StorageDiscovery storageDiscovery,
                            StorageFactory storageFactory) {
        val storages = storageDiscovery.discover();
        for (Class<?> storageInterface : storages) {
            try {
                Object proxy = storageFactory.create((Class) storageInterface);
                registry.registerSingletonBeanDefinition(storageInterface, proxy);
                log.debug("Registered storage interface: {}", storageInterface.getName());
            } catch (Exception e) {
                log.error("Failed to register storage interface: {}", storageInterface.getName(), e);
            }
        }
        log.info("Registered {} storages", storages.size());
    }
}