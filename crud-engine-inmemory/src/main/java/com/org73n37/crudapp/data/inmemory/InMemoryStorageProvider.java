package com.org73n37.crudapp.data.inmemory;

import com.org73n37.crudapp.data.core.BaseEntity;
import com.org73n37.crudapp.logic.spi.CrudStorageProvider;
import com.org73n37.crudapp.logic.core.CrudService.Page;
import com.org73n37.crudapp.infrastructure.security.TenantContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryStorageProvider<T extends BaseEntity> implements CrudStorageProvider<T> {
    private final Map<String, Map<Long, T>> db = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    private String getActiveTenantId() {
        String id = TenantContext.getTenantId();
        return (id != null) ? id : "default";
    }

    private Map<Long, T> getTenantStorage() {
        return db.computeIfAbsent(getActiveTenantId(), k -> new ConcurrentHashMap<>());
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(getTenantStorage().values());
    }

    @Override
    public Page<T> findAll(int offset, int limit, Map<String, List<String>> queryParams, String sortParam, Class<?> dtoClass) {
        List<T> all = findAll();
        int toIndex = Math.min(offset + limit, all.size());
        if (offset > all.size()) {
            return new Page<>(List.of(), all.size());
        }
        return new Page<>(all.subList(offset, toIndex), all.size());
    }

    @Override
    public Optional<T> findById(Long id) {
        return Optional.ofNullable(getTenantStorage().get(id));
    }

    @Override
    public T save(T entity) {
        String tenantId = getActiveTenantId();
        entity.setTenantId(tenantId);
        if (entity.getId() == null) {
            entity.setId(idSequence.getAndIncrement());
        }
        getTenantStorage().put(entity.getId(), entity);
        return entity;
    }

    @Override
    public void deleteById(Long id) {
        getTenantStorage().remove(id);
    }

    @Override
    public boolean existsById(Long id) {
        return getTenantStorage().containsKey(id);
    }

    @Override
    public long count() {
        return getTenantStorage().size();
    }
}
