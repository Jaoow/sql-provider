package com.jaoow.sql.executor.exceptions;

import com.jaoow.sql.executor.SQLExecutor;

/**
 * Exception called when the class of adapter was
 * not found in @{@link SQLExecutor}
 */
public class SQLAdapterNotFoundException extends SQLExecutorException {

    /**
     * Create an exception without cause
     */
    public SQLAdapterNotFoundException() {
        super();
    }

    /**
     * Create an exception with cause
     * @param message the cause
     */
    public SQLAdapterNotFoundException(String message) {
        super(message);
    }

}
