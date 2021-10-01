package com.jaoow.sql.connector.type.impl;

import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.connector.type.SQLDatabaseType;
import lombok.Builder;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Getter
public final class SQLiteDatabaseType extends SQLDatabaseType {

    private final File file;

    public SQLiteDatabaseType(String driverClassName, String jdbcUrl, File file) {
        super(driverClassName, jdbcUrl);
        this.file = file;
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
        Connection connection = DriverManager.getConnection(this.getJdbcUrl());
        return consumer -> consumer.accept(connection);
    }
}
