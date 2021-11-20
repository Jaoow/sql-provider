package com.jaoow.sql.executor.result;

import com.jaoow.sql.executor.exceptions.SQLExecutorException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

@SuppressWarnings("unchecked")
@RequiredArgsConstructor(staticName = "of", onConstructor_ = {@NotNull})
public final class SimpleResultSet implements AutoCloseable {

    private final ResultSet resultSet;

    public <T> T get(String column) {
        try {
            if (resultSet.isBeforeFirst()) {
                throw new UnsupportedOperationException("ResultSet hasn't any result, use next() to search first result!");
            }

            return (T) resultSet.getObject(column);

        } catch (SQLException e) {
            throw new SQLExecutorException("\"" + column + "\" no has element (" + e.getMessage() + ").");
        }
    }

    public boolean next() {
        try {
            return this.resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void close() throws SQLException {
        resultSet.close();
    }
}
