package com.org73n37.crudapp.logic.core;

import com.org73n37.crudapp.infrastructure.security.TenantContext;
import com.org73n37.crudapp.logic.spi.TenantIsolationHandler;
import org.springframework.stereotype.Component;

@Component
public class ThreadLocalTenantIsolationHandler implements TenantIsolationHandler {
    @Override
    public void setTenantContext(String tenantId) {
        TenantContext.setTenantId(tenantId);
    }

    @Override
    public void clearTenantContext() {
        TenantContext.clear();
    }
}
