package com.jaoow.sql.executor;

import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.executor.adapter.ResultSetAdapter;
import com.jaoow.sql.executor.adapter.SQLResultAdapter;
import com.jaoow.sql.executor.result.SimpleResultSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class SQLExecutorBuilder {

    private final SQLConnector connector;
    private final Map<Class<?>, SQLResultAdapter<?>> adapters = new HashMap<>();

    private Executor executor;

    public SQLExecutorBuilder(SQLConnector connector) {
        this.connector = connector;
        this.adapters.put(SimpleResultSet.class, new ResultSetAdapter());
    }

    public SQLExecutorBuilder setExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public <T> SQLExecutorBuilder registerAdapter(Class<T> clazz, SQLResultAdapter<T> adapter) {
        adapters.put(clazz, adapter);
        return this;
    }

    public SQLExecutor build() {
        Map<Class<?>, SQLResultAdapter<?>> immutable = Collections.unmodifiableMap(adapters);
        return executor == null ?
                new SQLExecutor(connector, immutable) :
                new SQLExecutor(connector, immutable, executor);

    }

}
