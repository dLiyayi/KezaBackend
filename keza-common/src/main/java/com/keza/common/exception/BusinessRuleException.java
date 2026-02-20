package com.keza.common.exception;

public class BusinessRuleException extends KezaException {

    public BusinessRuleException(String message) {
        super("BUSINESS_RULE_VIOLATION", message);
    }

    public BusinessRuleException(String code, String message) {
        super(code, message);
    }
}
