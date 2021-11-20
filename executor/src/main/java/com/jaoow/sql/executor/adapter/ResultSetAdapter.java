package com.jaoow.sql.executor.adapter;

import com.jaoow.sql.executor.result.SimpleResultSet;

public class ResultSetAdapter implements SQLResultAdapter<SimpleResultSet> {

    @Override
    public SimpleResultSet adaptResult(SimpleResultSet record) {
        return record;
    }

}
