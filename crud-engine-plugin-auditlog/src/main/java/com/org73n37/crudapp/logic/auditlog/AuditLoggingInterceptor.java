package com.org73n37.crudapp.logic.auditlog;

import com.org73n37.crudapp.data.core.BaseEntity;
import com.org73n37.crudapp.infrastructure.security.TenantContext;
import com.org73n37.crudapp.logic.core.CrudInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class AuditLoggingInterceptor implements CrudInterceptor<BaseEntity> {
    private static final Logger log = LoggerFactory.getLogger(AuditLoggingInterceptor.class);

    @Override
    public void beforeCreate(BaseEntity entity) {
        log.info("[AUDIT LOG - BEFORE_CREATE] Entity={} TenantId={} User={}",
                entity.getClass().getSimpleName(),
                entity.getTenantId() != null ? entity.getTenantId() : TenantContext.getTenantId(),
                entity.getCreatedBy());
    }

    @Override
    public void afterCreate(BaseEntity entity) {
        log.info("[AUDIT LOG - AFTER_CREATE] Entity={} Id={} TenantId={} User={}",
                entity.getClass().getSimpleName(),
                entity.getId(),
                entity.getTenantId(),
                entity.getCreatedBy());
    }

    @Override
    public void beforeUpdate(BaseEntity entity) {
        log.info("[AUDIT LOG - BEFORE_UPDATE] Entity={} Id={} TenantId={} User={}",
                entity.getClass().getSimpleName(),
                entity.getId(),
                entity.getTenantId() != null ? entity.getTenantId() : TenantContext.getTenantId(),
                entity.getLastModifiedBy());
    }

    @Override
    public void afterUpdate(BaseEntity entity) {
        log.info("[AUDIT LOG - AFTER_UPDATE] Entity={} Id={} TenantId={} User={}",
                entity.getClass().getSimpleName(),
                entity.getId(),
                entity.getTenantId(),
                entity.getLastModifiedBy());
    }

    @Override
    public void beforeDelete(Long id) {
        log.info("[AUDIT LOG - BEFORE_DELETE] Id={} TenantId={}",
                id,
                TenantContext.getTenantId());
    }

    @Override
    public void afterDelete(Long id) {
        log.info("[AUDIT LOG - AFTER_DELETE] Id={} TenantId={}",
                id,
                TenantContext.getTenantId());
    }
}
