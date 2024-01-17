package com.jaoow.sql.connector.type.impl;

import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.connector.type.SQLDatabaseType;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@Getter
public final class SQLiteDatabaseType extends SQLDatabaseType {

    private final SQLiteDataSource source;
    private final File file;

    public SQLiteDatabaseType(@NotNull String driverClassName, @NotNull String jdbcUrl, @NotNull File file) {
        super(driverClassName, jdbcUrl);
        this.file = file;

        SQLiteConfig config = new SQLiteConfig();
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        config.setTempStore(SQLiteConfig.TempStore.MEMORY);
        config.setPageSize(32768);

        source = new SQLiteDataSource(config);
        source.setUrl("jdbc:sqlite:" + file);
    }

    @Builder
    public SQLiteDatabaseType(@NotNull File file) {
        this(
                "org.sqlite.JDBC",
                "jdbc:sqlite:" + file,
                file
        );

        try {
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException("The database folder cannot be created.");
            }

            if (!file.exists() && !file.createNewFile()) {
                throw new IOException("The database file cannot be created.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    @NotNull
    public SQLConnector connect() throws SQLException {

        try {
            Class.forName(this.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver not found.");
        }

        return consumer -> {
            try (Connection connection = source.getConnection()) {
                consumer.execute(connection);
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        };
    }
}
