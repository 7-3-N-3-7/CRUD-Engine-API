package com.example.crudapp.data.core;

import com.example.crudapp.infrastructure.security.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class CrudRepository<T extends BaseEntity> {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private Class<T> entityClass;

    public CrudRepository() {
    }

    public CrudRepository(Class<T> entityClass, EntityManager entityManager) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
    }

    public void setEntityClass(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    private String getActiveTenantId() {
        String id = TenantContext.getTenantId();
        return (id != null) ? id : "default";
    }

    public List<T> findAll() {
        String tenantId = getActiveTenantId();
        return entityManager.createQuery(
                "SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e.tenantId = :tenantId", entityClass)
                .setParameter("tenantId", tenantId)
                .getResultList();
    }

    public List<T> findAll(int offset, int limit) {
        String tenantId = getActiveTenantId();
        TypedQuery<T> query = entityManager.createQuery(
                "SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e.tenantId = :tenantId", entityClass)
                .setParameter("tenantId", tenantId);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    public Optional<T> findById(Long id) {
        String tenantId = getActiveTenantId();
        T entity = entityManager.find(entityClass, id);
        if (entity != null && tenantId.equals(entity.getTenantId())) {
            return Optional.of(entity);
        }
        return Optional.empty();
    }

    public T save(T entity) {
        String tenantId = getActiveTenantId();
        
        if (entity.getId() == null) {
            entity.setTenantId(tenantId);
            entityManager.persist(entity);
            return entity;
        } else {
            T existing = entityManager.find(entityClass, entity.getId());
            if (existing != null && !tenantId.equals(existing.getTenantId())) {
                throw new SecurityException("Unauthorized access to entity under different tenant");
            }
            entity.setTenantId(tenantId);
            return entityManager.merge(entity);
        }
    }

    public void deleteById(Long id) {
        String tenantId = getActiveTenantId();
        T entity = entityManager.find(entityClass, id);
        if (entity != null && tenantId.equals(entity.getTenantId())) {
            entityManager.remove(entity);
        }
    }

    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    public long count() {
        String tenantId = getActiveTenantId();
        return entityManager.createQuery(
                "SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e WHERE e.tenantId = :tenantId", Long.class)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
    }
}
