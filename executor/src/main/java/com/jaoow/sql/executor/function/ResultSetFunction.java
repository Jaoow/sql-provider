package com.jaoow.sql.executor.function;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This is a functional interface that accepts the {@link ResultSet}, without
 * needing to handle the {@link SQLException} error, to return a result.
 *
 * @param <R> The type of result.
 */
@FunctionalInterface
public interface ResultSetFunction<R> {

    /**
     * Apply this function to @{@link ResultSet}.
     *
     * @param result The @{@link ResultSet}.
     * @return The result.
     */
    R apply(@NotNull ResultSet result) throws SQLException;

}