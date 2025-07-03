package xyz.quartzframework.data.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import xyz.quartzframework.core.bean.registry.PluginBeanDefinitionRegistry;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class StorageRegistrar {

    @Getter
    private final List<StorageDefinition> storages = new ArrayList<>();

    public StorageRegistrar(PluginBeanDefinitionRegistry registry,
                            StorageDiscovery storageDiscovery,
                            StorageFactory storageFactory) {
        val storages = storageDiscovery.discover();
        for (Class<?> storageInterface : storages) {
            try {
                Object proxy = storageFactory.create((Class) storageInterface);
                val entity = storageFactory.resolveEntityType(storageInterface);
                val id = storageFactory.resolveIdType(storageInterface);
                registry.registerSingletonBeanDefinition(storageInterface, proxy);
                getStorages().add(new StorageDefinition(storageInterface, entity, id));
                log.debug("Registered storage interface: {}", storageInterface.getName());
            } catch (Exception e) {
                log.error("Failed to register storage interface: {}", storageInterface.getName(), e);
            }
        }
        log.info("Registered {} storages", storages.size());
    }
}