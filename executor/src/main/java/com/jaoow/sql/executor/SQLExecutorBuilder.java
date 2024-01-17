package com.jaoow.sql.executor;

import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.executor.adapter.SQLResultAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Class to build @{@link SQLExecutor}
 *
 * @deprecated create a @{@link SQLExecutor} instance instead
 */
@Deprecated
public class SQLExecutorBuilder {

    @NotNull
    private final SQLConnector connector;
    @NotNull
    private final Map<Class<?>, SQLResultAdapter<?>> adapters = new HashMap<>();

    @Nullable
    private Executor executor;

    /**
     * Create an instance of @{@link SQLExecutorBuilder}
     *
     * @param connector the @{@link SQLConnector}
     */
    public SQLExecutorBuilder(@NotNull SQLConnector connector) {
        this.connector = connector;
    }

    /**
     * Set the @{@link Executor} of asynchronous threads.
     *
     * @param executor the @{@link Executor}
     * @return the @{@link SQLExecutorBuilder}
     */
    @NotNull
    public SQLExecutorBuilder setExecutor(@Nullable Executor executor) {
        this.executor = executor;
        return this;
    }

    /**
     * Register adapters to map queries.
     *
     * @param clazz   the class of adapter
     * @param adapter the @{@link SQLResultAdapter} of clazz
     * @param <T>     the type
     * @return the @{@link SQLExecutorBuilder}
     */
    @NotNull
    public <T> SQLExecutorBuilder registerAdapter(@NotNull Class<T> clazz, @NotNull SQLResultAdapter<T> adapter) {
        adapters.put(clazz, adapter);
        return this;
    }

    /**
     * Build @{@link SQLExecutor}
     *
     * @return the @{@link SQLExecutor}
     */
    @NotNull
    public SQLExecutor build() {
        Map<Class<?>, SQLResultAdapter<?>> immutable = Collections.unmodifiableMap(adapters);
        return executor == null ?
                new SQLExecutor(connector, immutable) :
                new SQLExecutor(connector, immutable, executor);
    }
}
