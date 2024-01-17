package com.jaoow.sql.connector;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface SQLConnector {

    void execute(@NotNull ConnectionConsumer connection);

}