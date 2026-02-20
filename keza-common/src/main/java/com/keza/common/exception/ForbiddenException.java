package com.keza.common.exception;

public class ForbiddenException extends KezaException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
