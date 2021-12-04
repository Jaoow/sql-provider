package com.jaoow.sql.executor.exceptions;

import com.jaoow.sql.executor.SQLExecutor;

/**
 * Exception called when the class of adapter was
 * not found in @{@link SQLExecutor}
 */
public class SQLAdapterNotFoundException extends SQLExecutorException {

    /**
     * Create an exception with name of invalid class
     *
     * @param clazz the invalid class
     */
    public SQLAdapterNotFoundException(Class<?> clazz) {
        super("The adapter for class " + clazz.getSimpleName() + " was not found.");
    }
}
