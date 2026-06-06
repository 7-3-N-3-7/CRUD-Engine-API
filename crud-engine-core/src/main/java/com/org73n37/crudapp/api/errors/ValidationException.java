package com.org73n37.crudapp.api.errors;

import jakarta.validation.ConstraintViolation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ValidationException extends RuntimeException {
    private final Map<String, String> errors;

    public ValidationException(Set<ConstraintViolation<Object>> violations) {
        super("Validation failed");
        this.errors = new HashMap<>();
        for (ConstraintViolation<Object> violation : violations) {
            errors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
