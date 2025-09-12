package com.agenticcp.core.common.exception;

public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String field, String message) {
        super(String.format("Validation failed for field '%s': %s", field, message));
    }
}
