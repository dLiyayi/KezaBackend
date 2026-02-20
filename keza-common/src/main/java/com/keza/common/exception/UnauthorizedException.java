package com.keza.common.exception;

public class UnauthorizedException extends KezaException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}
