package com.org73n37.crudapp.logic.spi;

public interface TenantIsolationHandler {
    void setTenantContext(String tenantId);
    void clearTenantContext();
}
