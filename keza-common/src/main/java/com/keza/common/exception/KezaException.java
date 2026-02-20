package com.keza.common.exception;

import lombok.Getter;

@Getter
public class KezaException extends RuntimeException {

    private final String code;

    public KezaException(String message) {
        super(message);
        this.code = "INTERNAL_ERROR";
    }

    public KezaException(String code, String message) {
        super(message);
        this.code = code;
    }

    public KezaException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
