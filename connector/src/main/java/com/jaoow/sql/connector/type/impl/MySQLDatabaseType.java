package com.jaoow.sql.connector.type.impl;

import com.jaoow.sql.connector.exception.ConnectorException;
import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.connector.type.SQLDatabaseType;
import com.zaxxer.hikari.HikariDataSource;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@SuperBuilder
public class MySQLDatabaseType extends SQLDatabaseType {

    // https://github.com/lucko/helper/blob/master/helper-sql/src/main/java/me/lucko/helper/sql/plugin/HelperSql.java
    private static final int MAXIMUM_POOL_SIZE = (Runtime.getRuntime().availableProcessors() * 2) + 1;
    private static final int MINIMUM_IDLE = Math.min(MAXIMUM_POOL_SIZE, 10);

    private static final long MAX_LIFETIME = TimeUnit.MINUTES.toMillis(30);
    private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private static final long LEAK_DETECTION_THRESHOLD = TimeUnit.SECONDS.toMillis(10);

    @NonNull private final String address;
    @NonNull private final String username;
    @NonNull private final String password;
    @NonNull private final String database;

    private HikariDataSource dataSource;

    public MySQLDatabaseType(@NonNull String address, @NonNull String username, @NonNull String password, @NonNull String database) {
        super("com.mysql.cj.jdbc.Driver", "jdbc:mysql://%s/%s");

        this.address = address;
        this.username = username;
        this.password = password;
        this.database = database;
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:mysql://%s/%s";
    }

    @Override
    public String getDriverClassName() {
        try {
            return Class.forName("com.mysql.cj.jdbc.Driver").getName();
        } catch (ClassNotFoundException exception) {
            return "com.mysql.jdbc.Driver";
        }
    }

    @Deprecated
    @Contract("_ -> this")
    public SQLDatabaseType configureDataSource(@NotNull Consumer<HikariDataSource> consumer) {
        consumer.accept(dataSource);
        return this;
    }

    @NotNull
    @Override
    public SQLConnector connect() throws SQLException {
        if (this.dataSource == null) {
            this.initDataSource();
        }

        // Test if connection was established.
        dataSource.getConnection().close();

        return consumer -> {
            try (Connection connection = dataSource.getConnection()) {
                consumer.execute(connection);
            } catch (SQLException exception) {
                throw new ConnectorException(exception);
            }
        };

    }

    private void initDataSource() {
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(String.format(this.getJdbcUrl(), address, database));
        dataSource.setDriverClassName(this.getDriverClassName());

        dataSource.setUsername(username);
        dataSource.setPassword(password);

        dataSource.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        dataSource.setMinimumIdle(MINIMUM_IDLE);

        dataSource.setMaxLifetime(MAX_LIFETIME);
        dataSource.setConnectionTimeout(CONNECTION_TIMEOUT);
        dataSource.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD);

        dataSource.addDataSourceProperty("useUnicode", true);
        dataSource.addDataSourceProperty("characterEncoding", "utf8");

        dataSource.addDataSourceProperty("cachePrepStmts", "true");
        dataSource.addDataSourceProperty("prepStmtCacheSize", "250");
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource.addDataSourceProperty("useServerPrepStmts", "true");
        dataSource.addDataSourceProperty("useLocalSessionState", "true");
        dataSource.addDataSourceProperty("rewriteBatchedStatements", "true");
        dataSource.addDataSourceProperty("cacheResultSetMetadata", "true");
        dataSource.addDataSourceProperty("cacheServerConfiguration", "true");
        dataSource.addDataSourceProperty("elideSetAutoCommits", "true");
        dataSource.addDataSourceProperty("maintainTimeStats", "false");
        dataSource.addDataSourceProperty("alwaysSendSetIsolation", "false");
        dataSource.addDataSourceProperty("cacheCallableStmts", "true");

        dataSource.addDataSourceProperty("socketTimeout", String.valueOf(TimeUnit.SECONDS.toMillis(30)));
    }

    @SuppressWarnings("unused")
    public static abstract class MySQLDatabaseTypeBuilder<C extends MySQLDatabaseType, B extends MySQLDatabaseTypeBuilder<C, B>> extends SQLDatabaseTypeBuilder<C, B> {

        protected B dataSource(HikariDataSource dataSource) {
            this.self().dataSource(dataSource);
            return self();
        }

    }

}
