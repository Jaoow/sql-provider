package com.jaoow.sql.executor.adapter;

import com.jaoow.sql.executor.result.SimpleResultSet;
import com.jaoow.sql.executor.statement.SimpleStatement;

public class ResultSetAdapter implements SQLResultAdapter<SimpleResultSet>{

    @Override
    public SimpleResultSet adaptResult(SimpleResultSet record) {
        return record;
    }

    @Override
    public void insert(SimpleStatement statement, SimpleResultSet value) {}

    @Override
    public void update(SimpleStatement statement, SimpleResultSet value) {}
}
