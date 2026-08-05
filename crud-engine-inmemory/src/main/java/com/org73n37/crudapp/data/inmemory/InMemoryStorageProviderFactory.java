package com.org73n37.crudapp.data.inmemory;

import com.org73n37.crudapp.data.core.BaseEntity;
import com.org73n37.crudapp.logic.spi.CrudStorageProvider;
import com.org73n37.crudapp.logic.spi.CrudStorageProviderFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class InMemoryStorageProviderFactory implements CrudStorageProviderFactory {
    private final Map<Class<?>, CrudStorageProvider<?>> providers = new ConcurrentHashMap<>();

    @Override
    public boolean supports(Class<? extends BaseEntity> entityClass) {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BaseEntity> CrudStorageProvider<T> getStorageProvider(Class<T> entityClass) {
        return (CrudStorageProvider<T>) providers.computeIfAbsent(entityClass, k -> new InMemoryStorageProvider<T>());
    }
}
