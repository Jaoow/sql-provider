package com.jaoow.sql.connector.type.impl;

import com.jaoow.sql.connector.exception.ConnectorException;
import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.connector.type.SQLDatabaseType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@Getter
@SuperBuilder
public final class SQLiteDatabaseType extends SQLDatabaseType {

    private final File file;

    private SQLiteDataSource source;

    public SQLiteDatabaseType(@NotNull String driverClassName, @NotNull String jdbcUrl, @NotNull File file) {
        super(driverClassName, jdbcUrl);
        this.file = file;
    }

    @NotNull
    @Override
    @Contract(pure = true)
    public String getJdbcUrl() {
        return "jdbc:sqlite:%s";
    }

    @NotNull
    @Override
    @Contract(pure = true)
    public String getDriverClassName() {
        return "org.sqlite.JDBC";
    }

    public static SQLiteDatabaseTypeBuilder<?,?> builder(@NotNull File file) {
        return new SQLiteDatabaseTypeBuilderImpl().file(file);
    }

    @NotNull
    @Override
    public SQLConnector connect() throws SQLException {
        try {
            Class.forName(this.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver not found.");
        }

        if (this.source == null) {
            try {
                this.initDataSource();
            } catch (IOException e) {
                throw new RuntimeException("Error while initializing the database file.", e);
            }
        }

        return consumer -> {
            try (Connection connection = source.getConnection()) {
                consumer.execute(connection);
            } catch (SQLException exception) {
                throw new ConnectorException(exception);
            }
        };
    }

    private void initDataSource() throws IOException {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("The database folder cannot be created.");
        }

        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("The database file cannot be created.");
        }

        SQLiteConfig config = new SQLiteConfig();
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        config.setTempStore(SQLiteConfig.TempStore.MEMORY);
        config.setPageSize(32768);

        source = new SQLiteDataSource(config);
        source.setUrl(String.format(getJdbcUrl(), file));
    }

}
