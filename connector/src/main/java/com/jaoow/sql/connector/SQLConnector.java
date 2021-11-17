package com.jaoow.sql.connector;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.function.Consumer;

@FunctionalInterface
public interface SQLConnector {

    void execute(@NotNull Consumer<Connection> consumer);

}
