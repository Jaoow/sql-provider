package com.jaoow.sql.executor.statement;

import com.jaoow.sql.executor.result.SimpleResultSet;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@RequiredArgsConstructor(staticName = "of", onConstructor_ = {@NotNull})
public final class SimpleStatement implements AutoCloseable {

    private final PreparedStatement preparedStatement;

    public void set(int parameterIndex, Object value) {
        try {
            preparedStatement.setObject(parameterIndex, value);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void execute() {
        try {
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void executeUpdate() {
        try {
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public SimpleResultSet executeQuery() {

        try {
            return SimpleResultSet.of(preparedStatement.executeQuery());
        } catch (SQLException e) {
            throw new NullPointerException("ResultSet cannot be null (" + e.getMessage() + ").");
        }

    }

    @Override
    public void close() throws SQLException {
        preparedStatement.close();
    }
}
