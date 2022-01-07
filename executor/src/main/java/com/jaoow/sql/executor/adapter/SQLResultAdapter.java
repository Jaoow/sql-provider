package com.jaoow.sql.executor.adapter;

import com.jaoow.sql.executor.result.SimpleResultSet;
import com.jaoow.sql.executor.statement.SimpleStatement;

import java.util.function.Consumer;

@FunctionalInterface
public interface SQLResultAdapter<T> {

    T adaptResult(SimpleResultSet record);

    default Consumer<SimpleStatement> adaptStatement(T value) {
        throw new UnsupportedOperationException("This adapter does not support this operation.");
    }
}