package xyz.quartzframework.data.entity;

import jakarta.persistence.Id;
import lombok.RequiredArgsConstructor;
import lombok.val;
import xyz.quartzframework.core.context.AbstractQuartzContext;
import xyz.quartzframework.core.util.ClassUtil;
import xyz.quartzframework.core.util.ReflectionUtil;
import xyz.quartzframework.data.annotation.DiscoverEntities;
import xyz.quartzframework.data.annotation.Identity;

import java.util.*;

@RequiredArgsConstructor
public class EntityDiscovery {

    private final AbstractQuartzContext<?> context;

    private final Set<String> basePackages = new HashSet<>();

    private final Set<Class<?>> explicitEntities = new HashSet<>();

    private Map<String, Object> discoverers = Map.of();

    public static EntityDiscovery builder(AbstractQuartzContext<?> context) {
        return new EntityDiscovery(context);
    }

    public EntityDiscovery addBasePackage(String basePackage) {
        this.basePackages.add(basePackage);
        return this;
    }

    public EntityDiscovery addBasePackages(Collection<String> packages) {
        this.basePackages.addAll(packages);
        return this;
    }

    public EntityDiscovery addExplicitEntities(Collection<Class<?>> storages) {
        this.explicitEntities.addAll(storages);
        return this;
    }

    public EntityDiscovery addDiscoverers(Map<String, Object> discoverers) {
        this.discoverers = discoverers;
        return this;
    }

    public Set<Class<?>> discover() {
        Set<Class<?>> entities = new HashSet<>(explicitEntities);
        val quartzApplication = context.getQuartzApplication();

        for (Object discoverer : discoverers.values()) {
            DiscoverEntities config = discoverer.getClass().getAnnotation(DiscoverEntities.class);
            if (config != null) {
                entities.addAll(Arrays.asList(config.value()));
                Collections.addAll(basePackages, config.basePackages());
            }
        }

        for (String pkg : basePackages) {
            entities.addAll(ClassUtil.scan(
                new String[]{pkg},
                quartzApplication.exclude(),
                c -> !c.isInterface() && !ReflectionUtil.getFields(c, Id.class, Identity.class).isEmpty(),
                quartzApplication.verbose()
            ));
        }

        if (entities.isEmpty()) {
            String fallback = context.getPluginClass().getPackageName();
            entities.addAll(ClassUtil.scan(
                new String[]{fallback},
                quartzApplication.exclude(),
                    c -> !c.isInterface() && !ReflectionUtil.getFields(c, Id.class, Identity.class).isEmpty(),
                quartzApplication.verbose()
            ));
        }

        return entities;
    }
}