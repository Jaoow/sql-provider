package com.jaoow.sql.executor.adapter;

import com.jaoow.sql.executor.result.SimpleResultSet;

@FunctionalInterface
public interface SQLResultAdapter<T> {

    T adaptResult(SimpleResultSet record);

}