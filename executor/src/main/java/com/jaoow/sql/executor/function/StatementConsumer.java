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
     * Performs this operation with the @{@link PreparedStatement}.
     *
     * @param statement The @{@link PreparedStatement}.
     */
    void accept(@NotNull PreparedStatement statement) throws SQLException;

}
