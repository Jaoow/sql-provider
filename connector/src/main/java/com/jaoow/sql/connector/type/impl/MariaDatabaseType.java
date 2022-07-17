package com.jaoow.sql.connector.type.impl;

import org.jetbrains.annotations.NotNull;

public class MariaDatabaseType extends MySQLDatabaseType {

    public MariaDatabaseType(@NotNull String address, @NotNull String username,
                             @NotNull String password, @NotNull String database) {

        super(address, username, password, database);
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:mariadb://%s/%s";
    }

    @Override
    public String getDriverClassName() {
        return "org.mariadb.jdbc.Driver";
    }

}