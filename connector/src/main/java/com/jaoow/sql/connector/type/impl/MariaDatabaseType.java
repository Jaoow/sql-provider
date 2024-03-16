package com.jaoow.sql.connector.type.impl;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class MariaDatabaseType extends MySQLDatabaseType {

    public MariaDatabaseType(@NonNull String address, @NonNull String username, @NonNull String password, @NonNull String database) {
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