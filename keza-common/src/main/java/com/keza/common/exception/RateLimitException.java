package com.keza.common.exception;

public class RateLimitException extends KezaException {

    public RateLimitException() {
        super("RATE_LIMITED", "Too many requests. Please try again later.");
    }

    public RateLimitException(String message) {
        super("RATE_LIMITED", message);
    }
}
