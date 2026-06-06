package com.org73n37.crudapp.logic;

import com.org73n37.crudapp.data.core.BaseEntity;
import com.org73n37.crudapp.logic.core.CrudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * [ARCHITECTURAL OPTIMIZATION]
 * Decouples the controller layer from service layer instantiation.
 * Checks the Spring ApplicationContext for custom CrudService beans first before falling back to dynamic default services.
 */
@Component
public class ServiceRegistry implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(ServiceRegistry.class);
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseEntity> CrudService<T> getService(Class<T> entityClass, CrudService<T> fallbackService) {
        if (applicationContext == null) {
            return fallbackService;
        }

        // Search for any CrudService bean registered in the Spring context
        Map<String, CrudService> serviceBeans = applicationContext.getBeansOfType(CrudService.class);
        for (CrudService<?> service : serviceBeans.values()) {
            // Check if the service matches our entity type
            Class<?>[] types = org.springframework.core.GenericTypeResolver.resolveTypeArguments(
                    service.getClass(),
                    CrudService.class
            );
            if (types != null && types.length > 0 && types[0].equals(entityClass)) {
                log.debug("Found custom service bean {} for entity {}", service.getClass().getSimpleName(), entityClass.getSimpleName());
                return (CrudService<T>) service;
            }
        }

        return fallbackService;
    }
}
