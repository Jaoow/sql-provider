package com.jaoow.sql.connector.type;

import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.connector.type.impl.MySQLDatabaseType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.sql.SQLException;

@Getter
@SuperBuilder
@RequiredArgsConstructor
public abstract class SQLDatabaseType {

    private final String driverClassName;
    private final String jdbcUrl;

    public abstract SQLConnector connect() throws SQLException;

    public static abstract class SQLDatabaseTypeBuilder<C extends SQLDatabaseType, B extends SQLDatabaseTypeBuilder<C, B>> {

        protected B driverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
            return self();
        }

        protected B jdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return self();
        }

    }

}
