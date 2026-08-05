package com.org73n37.crudapp.data.weaviate;

import tools.jackson.databind.json.JsonMapper;
import io.weaviate.client6.v1.api.collections.query.Filter;
import com.org73n37.crudapp.data.core.BaseEntity;
import com.org73n37.crudapp.logic.spi.CrudStorageProvider;
import com.org73n37.crudapp.logic.core.CrudService.Page;
import com.org73n37.crudapp.infrastructure.security.TenantContext;
import io.weaviate.client6.v1.api.WeaviateClient;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class WeaviateStorageProvider<T extends BaseEntity> implements CrudStorageProvider<T> {
    private final Class<T> entityClass;
    private final WeaviateClient client;
    private final String collectionName;
    private final JsonMapper objectMapper;
    private final AtomicLong idSequence = new AtomicLong(1);

    public WeaviateStorageProvider(Class<T> entityClass, WeaviateClient client, String collectionName) {
        this.entityClass = entityClass;
        this.client = client;
        this.collectionName = collectionName;
        this.objectMapper = JsonMapper.builder()
                .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        ensureCollectionExists();
        initializeSequence();
    }

    private void ensureCollectionExists() {
        try {
            if (!client.collections.exists(collectionName)) {
                client.collections.create(collectionName, builder -> builder
                        .description("Collection for entity " + entityClass.getSimpleName())
                );
            }
        } catch (Exception e) {
            // Silence or log
        }
    }

    private void initializeSequence() {
        try {
            var collection = client.collections.use(collectionName);
            var result = collection.query.fetchObjects();
            long maxId = 0;
            if (result != null && result.objects() != null) {
                for (var obj : result.objects()) {
                    Object idVal = obj.properties().get("entityId");
                    if (idVal instanceof Number number) {
                        maxId = Math.max(maxId, number.longValue());
                    }
                }
            }
            idSequence.set(maxId + 1);
        } catch (Exception e) {
            idSequence.set(System.currentTimeMillis());
        }
    }

    private String getActiveTenantId() {
        String id = TenantContext.getTenantId();
        return (id != null) ? id : "default";
    }

    private UUID getUuidFromLong(Long id) {
        if (id == null) {
            return null;
        }
        return UUID.nameUUIDFromBytes(String.valueOf(id).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public List<T> findAll() {
        try {
            var collection = client.collections.use(collectionName);
            var result = collection.query.fetchObjects();
            List<T> list = new ArrayList<>();
            if (result != null && result.objects() != null) {
                for (var obj : result.objects()) {
                    T entity = objectMapper.convertValue(obj.properties(), entityClass);
                    // Map tenant check
                    if (getActiveTenantId().equals(entity.getTenantId())) {
                        list.add(entity);
                    }
                }
            }
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
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
        try {
            UUID uuid = getUuidFromLong(id);
            var collection = client.collections.use(collectionName);
            var response = collection.query.fetchObjectById(uuid.toString());
            if (response.isPresent() && response.get().properties() != null) {
                T entity = objectMapper.convertValue(response.get().properties(), entityClass);
                if (getActiveTenantId().equals(entity.getTenantId())) {
                    return Optional.of(entity);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T save(T entity) {
        try {
            String tenantId = getActiveTenantId();
            entity.setTenantId(tenantId);
            if (entity.getId() == null) {
                entity.setId(idSequence.getAndIncrement());
            }
            UUID uuid = getUuidFromLong(entity.getId());
            var collection = client.collections.use(collectionName);

            // Serialize entity properties to Map
            Map<String, Object> properties = objectMapper.convertValue(entity, Map.class);
            // Weaviate requires properties to not have "id" (or it will clash).
            // Let's store "id" as "entityId".
            properties.remove("id");
            properties.put("entityId", entity.getId());

            // Check if object already exists to update it, otherwise insert it
            if (existsById(entity.getId())) {
                collection.data.update(uuid.toString(), builder -> builder.properties(properties));
            } else {
                collection.data.insert(properties, builder -> builder.uuid(uuid.toString()));
            }
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save entity in Weaviate", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            UUID uuid = getUuidFromLong(id);
            var collection = client.collections.use(collectionName);
            collection.data.deleteMany(Filter.property("id").eq(uuid.toString()));
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    @Override
    public long count() {
        return findAll().size();
    }
}
