package com.jaoow.sql.connector;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * This is a functional interface that accepts the {@link Connection} without
 * needing to handle the {@link SQLException} error.
 */
@FunctionalInterface
public interface ConnectionConsumer {

    /**
     * Performs this operation with the @{@link Connection}.
     *
     * @param connection The connection.
     * @throws SQLException If an error occurs.
     */
    void execute(@NotNull Connection connection) throws SQLException;

}