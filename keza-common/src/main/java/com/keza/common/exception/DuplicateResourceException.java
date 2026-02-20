package com.keza.common.exception;

public class DuplicateResourceException extends KezaException {

    public DuplicateResourceException(String resource, String field, String value) {
        super("DUPLICATE_RESOURCE", String.format("%s already exists with %s: %s", resource, field, value));
    }
}
