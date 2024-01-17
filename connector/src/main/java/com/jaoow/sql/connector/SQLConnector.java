package com.jaoow.sql.connector;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

@FunctionalInterface
public interface SQLConnector {

    void execute(@NotNull ConnectionConsumer connection) throws SQLException;

}