package com.agenticdev.sdlc.coding.domain;

public class ContainerException extends CodingRunException {

    public ContainerException(String message, Throwable cause) {
        super("container_error", message, cause);
    }

    public ContainerException(String message) {
        super("container_error", message);
    }
}
