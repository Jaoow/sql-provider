package com.jaoow.sql.executor.adapter;

import java.sql.ResultSet;

@FunctionalInterface
public interface SQLResultAdapter<T> {

    T adaptResult(ResultSet record);

}