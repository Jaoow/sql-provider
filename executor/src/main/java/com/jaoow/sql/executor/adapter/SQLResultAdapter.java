package com.jaoow.sql.executor.adapter;

import com.jaoow.sql.executor.result.SimpleResultSet;
import com.jaoow.sql.executor.statement.SimpleStatement;

public interface SQLResultAdapter<T> {

    T adaptResult(SimpleResultSet record);

    void insert(SimpleStatement statement, T value);

    void update(SimpleStatement statement, T value);

}