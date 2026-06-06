package com.org73n37.crudapp.domain.product;

import com.org73n37.crudapp.data.Product;
import com.org73n37.crudapp.logic.core.CrudInterceptor;
import org.springframework.stereotype.Component;

/**
 * [DOMAIN LAYER]
 * Example of a custom business logic hook for Products.
 */
@Component
public class ProductInterceptor implements CrudInterceptor<Product> {

    @Override
    public void beforeCreate(Product product) {
        System.out.println("🧠 [LOGIC] Intercepting Product creation for: " + product.getName());
        // Custom business logic: ensure name is uppercase (example)
        if (product.getName() != null) {
            product.setName(product.getName().toUpperCase());
        }
    }

    @Override
    public void afterCreate(Product product) {
        System.out.println("✅ [LOGIC] Product created successfully with ID: " + product.getId());
    }
}
