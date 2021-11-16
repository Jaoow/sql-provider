package com.jaoow.sql.connector.type.impl;

import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.connector.type.SQLDatabaseType;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Builder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

public final class MySQLDatabaseType extends SQLDatabaseType {

    private final HikariDataSource dataSource = new HikariDataSource();

    @Builder
    public MySQLDatabaseType(String address, String username, String password, String database) {
        super(
                "com.mysql.jdbc.Driver",
                "jdbc:mysql://" + address + "/" + database);

        dataSource.setJdbcUrl(super.getJdbcUrl());
        dataSource.setDriverClassName(super.getDriverClassName());

        dataSource.setUsername(username);
        dataSource.setPassword(password);
    }

    @Contract("_ -> this")
    public SQLDatabaseType configureDataSource(@NotNull Consumer<HikariDataSource> consumer) {
        consumer.accept(dataSource);
        return this;
    }

    @NotNull
    @Override
    public SQLConnector connect() throws SQLException {

        // Test if connection was established.
        dataSource.getConnection().close();

        return consumer -> {
            try (Connection connection = dataSource.getConnection()) {
                consumer.accept(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };
    }
}
