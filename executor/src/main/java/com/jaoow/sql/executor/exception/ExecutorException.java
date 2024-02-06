package com.jaoow.sql.executor.exception;

import lombok.NonNull;

/**
 * Custom exception for throwing executor exceptions at runtime.
 */
public class ExecutorException extends RuntimeException {

    /**
     * Constructs a new executor runtime exception with null cause.
     */
    public ExecutorException() {
        super();
    }

    /**
     * Constructs a new executor runtime exception.
     *
     * @param cause The cause.
     */
    public ExecutorException(@NonNull Throwable cause) {
        super(cause);
    }

}
