package com.agenticdev.sdlc.coding.domain;

public class CodingRunException extends RuntimeException {

    private final String code;

    public CodingRunException(String code, String message) {
        super(message);
        this.code = code;
    }

    public CodingRunException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
