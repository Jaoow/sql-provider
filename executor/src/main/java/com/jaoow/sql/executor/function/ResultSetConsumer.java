package com.jaoow.sql.executor.function;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This is a functional interface that accepts the @{@link ResultSet}
 * to execute an operation without needing to handle the @{@link SQLException}.
 */
@FunctionalInterface
public interface ResultSetConsumer {

    /**
     * Performs this operation with the @{@link ResultSet}.
     *
     * @param result The @{@link ResultSet}.
     */
    void accept(@NotNull ResultSet result) throws SQLException;

}