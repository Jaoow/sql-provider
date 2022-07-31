package com.jaoow.sql.executor.adapter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface SQLResultAdapter<T> {

    @Nullable
    T adaptResult(@NotNull ResultSet record) throws SQLException;

}