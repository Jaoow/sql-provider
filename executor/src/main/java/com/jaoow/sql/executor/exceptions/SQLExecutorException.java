package com.jaoow.sql.executor.exceptions;

import com.jaoow.sql.executor.SQLExecutor;

/**
 * Exception called when an error occurs in execution of any
 * database statements in @{@link SQLExecutor}
 */
public class SQLExecutorException extends RuntimeException {

    /**
     * Create an exception without cause
     */
    public SQLExecutorException() {
        super();
    }

    /**
     * Create an exception with cause
     * @param message the cause
     */
    public SQLExecutorException(String message) {
        super(message);
    }

}
