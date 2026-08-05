package com.org73n37.crudapp.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testSetAndGetTenantId() {
        TenantContext.setTenantId("tenant-123");
        assertEquals("tenant-123", TenantContext.getTenantId(), "TenantContext should return the set tenant ID");
    }

    @Test
    void testClearTenantId() {
        TenantContext.setTenantId("tenant-abc");
        assertEquals("tenant-abc", TenantContext.getTenantId());
        TenantContext.clear();
        assertNull(TenantContext.getTenantId(), "Tenant ID should be null after clearing");
    }

    @Test
    void testThreadLocalIsolation() throws InterruptedException {
        TenantContext.setTenantId("main-thread-tenant");
        assertEquals("main-thread-tenant", TenantContext.getTenantId());

        AtomicReference<String> threadTenantId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            try {
                // Thread-local of child thread should start as null, not inherit from parent thread
                threadTenantId.set(TenantContext.getTenantId());
                TenantContext.setTenantId("child-thread-tenant");
            } finally {
                latch.countDown();
            }
        });

        thread.start();
        boolean completed = latch.await(2, TimeUnit.SECONDS);

        assertTrue(completed, "Child thread execution timed out");
        assertNull(threadTenantId.get(), "Child thread should initially have null tenant ID");
        assertEquals("main-thread-tenant", TenantContext.getTenantId(), "Main thread tenant ID should remain unchanged");
    }
}
