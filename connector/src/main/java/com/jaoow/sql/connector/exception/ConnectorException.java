package com.jaoow.sql.connector.exception;

import lombok.NonNull;

/**
 * Custom exception for throwing connector exceptions at runtime.
 */
public class ConnectorException extends RuntimeException {

    /**
     * Constructs a new connector runtime exception with null cause.
     */
    public ConnectorException() {
        super();
    }

    /**
     * Constructs a new connector runtime exception.
     *
     * @param cause The cause.
     */
    public ConnectorException(@NonNull Throwable cause) {
        super(cause);
    }

}