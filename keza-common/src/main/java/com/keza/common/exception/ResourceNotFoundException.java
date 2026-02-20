package com.keza.common.exception;

import java.util.UUID;

public class ResourceNotFoundException extends KezaException {

    public ResourceNotFoundException(String resource, UUID id) {
        super("RESOURCE_NOT_FOUND", String.format("%s not found with id: %s", resource, id));
    }

    public ResourceNotFoundException(String resource, String field, String value) {
        super("RESOURCE_NOT_FOUND", String.format("%s not found with %s: %s", resource, field, value));
    }
}
