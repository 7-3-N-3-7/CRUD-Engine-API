package com.org73n37.crudapp.data.weaviate;

import com.org73n37.crudapp.data.core.BaseEntity;
import com.org73n37.crudapp.data.weaviate.annotation.WeaviateEntity;
import com.org73n37.crudapp.logic.spi.CrudStorageProvider;
import com.org73n37.crudapp.logic.spi.CrudStorageProviderFactory;
import io.weaviate.client6.v1.api.WeaviateClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnBean(WeaviateClient.class)
public class WeaviateStorageProviderFactory implements CrudStorageProviderFactory {
    private final WeaviateClient client;
    private final Map<Class<?>, CrudStorageProvider<?>> providers = new ConcurrentHashMap<>();

    public WeaviateStorageProviderFactory(WeaviateClient client) {
        this.client = client;
    }

    @Override
    public boolean supports(Class<? extends BaseEntity> entityClass) {
        return entityClass.isAnnotationPresent(WeaviateEntity.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BaseEntity> CrudStorageProvider<T> getStorageProvider(Class<T> entityClass) {
        return (CrudStorageProvider<T>) providers.computeIfAbsent(entityClass, k -> {
            WeaviateEntity ann = entityClass.getAnnotation(WeaviateEntity.class);
            String name = (ann.value() != null && !ann.value().isBlank()) ? ann.value() : entityClass.getSimpleName();
            // Weaviate collection names must start with an uppercase letter
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            return new WeaviateStorageProvider<>(entityClass, client, name);
        });
    }
}
