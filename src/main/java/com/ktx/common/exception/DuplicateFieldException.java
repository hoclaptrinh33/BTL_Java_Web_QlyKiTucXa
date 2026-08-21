package com.ktx.common.exception;

public class DuplicateFieldException extends RuntimeException {

    private final String field;

    public DuplicateFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
