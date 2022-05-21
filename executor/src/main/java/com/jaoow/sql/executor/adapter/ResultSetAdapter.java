package com.jaoow.sql.executor.adapter;

import java.sql.ResultSet;

public class ResultSetAdapter implements SQLResultAdapter<ResultSet> {

    @Override
    public ResultSet adaptResult(ResultSet record) {
        return record;
    }

}
