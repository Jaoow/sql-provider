package com.jaoow.sql.connector.type.impl;

import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.connector.type.SQLDatabaseType;
import lombok.Builder;
import lombok.Getter;
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

    public SQLiteDatabaseType(String driverClassName, String jdbcUrl, File file) {
        super(driverClassName, jdbcUrl);
        this.file = file;

        SQLiteConfig config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        config.setTempStore(SQLiteConfig.TempStore.MEMORY);
        config.setPageSize(32768);
        config.setEncoding(SQLiteConfig.Encoding.UTF_8);

        source = new SQLiteDataSource(config);
        source.setUrl("jdbc:sqlite:" + file);
    }

    @Builder
    public SQLiteDatabaseType(File file) {
        this(
                "org.sqlite.JDBC",
                "jdbc:sqlite:" + file,
                file
        );

        try {
            File parent = file.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    throw new IOException("The database folder cannot be created.");
                }
            }

            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw new IOException("The database file cannot be created.");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SQLConnector connect() throws SQLException {

        try {
            Class.forName(this.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver not found.");
        }

        return consumer -> {
            try(Connection connection = source.getConnection()) {
                consumer.accept(connection);

            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        };
    }
}
