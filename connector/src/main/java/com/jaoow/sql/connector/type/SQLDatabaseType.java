package com.jaoow.sql.connector.type;

import com.jaoow.sql.connector.SQLConnector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;

@Getter
@RequiredArgsConstructor
public abstract class SQLDatabaseType {

    private final String driverClassName;
    private final String jdbcUrl;

    public abstract SQLConnector connect() throws SQLException;

}
