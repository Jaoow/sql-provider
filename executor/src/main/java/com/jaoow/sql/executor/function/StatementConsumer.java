package com.jaoow.sql.executor.function;

import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * This is a functional interface that accepts the @{@link PreparedStatement}
 * to execute an operation without needing to handle the @{@link SQLException}.
 */
@FunctionalInterface
public interface StatementConsumer {

    /**
     * An empty statement.
     */
    StatementConsumer EMPTY_STATEMENT = statement -> {
    };

    /**
     * Performs this operation with the @{@link PreparedStatement}.
     *
     * @param statement The statement.
     * @throws SQLException If an error occurs.
     */
    void accept(@NotNull PreparedStatement statement) throws SQLException;

}
