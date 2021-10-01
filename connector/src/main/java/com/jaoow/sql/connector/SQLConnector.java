package com.jaoow.sql.connector;

import java.sql.Connection;
import java.util.function.Consumer;

@FunctionalInterface
public interface SQLConnector {

    void execute(Consumer<Connection> consumer);

}
