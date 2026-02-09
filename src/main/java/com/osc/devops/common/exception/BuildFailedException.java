package com.osc.devops.common.exception;

public class BuildFailedException extends RuntimeException {
    public BuildFailedException(String message) {
        super(message);
    }
    public BuildFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
