package com.org73n37.crudapp.api.records;

import com.org73n37.crudapp.infrastructure.annotations.EntityMapping;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * [INTERFACE LAYER]
 * Immutable API representation of a Product, including hierarchy support and dynamic attributes.
 */
@EntityMapping(entity = com.org73n37.crudapp.data.Product.class)
public record ProductRecord(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,
    
    @Size(max = 500, message = "Description can't exceed 500 characters")
    String description,
    
    @Positive(message = "Price must be positive")
    Double price,
    
    Long parentId,
    Long grandparentId,
    
    Map<String, String> attributes
) {}
