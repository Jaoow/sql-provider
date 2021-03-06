package com.jaoow.sql.executor;

import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.executor.adapter.SQLResultAdapter;
import com.jaoow.sql.executor.batch.BatchBuilder;
import com.jaoow.sql.executor.function.ResultSetFunction;
import com.jaoow.sql.executor.function.StatementConsumer;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class to execute database statements more easily
 */
@RequiredArgsConstructor
@AllArgsConstructor
public final class SQLExecutor {

    private static final StatementConsumer EMPTY_STATEMENT = statement -> {};

    @NotNull
    private final SQLConnector sqlConnector;

    @NotNull
    private final Map<Class<?>, SQLResultAdapter<?>> adapters;

    @NotNull
    private Executor executor = ForkJoinPool.commonPool();

    /**
     * Create an instance of @{@link SQLExecutor}
     *
     * @param connector the @{@link SQLConnector}
     */
    public SQLExecutor(@NotNull SQLConnector connector) {
        this.sqlConnector = connector;
        this.adapters = new HashMap<>();
    }

    /**
     * Set the executor to perform asynchronous statements
     *
     * @param executor tbe @{@link Executor}
     */
    public void setExecutor(@NotNull Executor executor) {
        this.executor = executor;
    }

    /**
     * Get the registered @{@link SQLResultAdapter}
     *
     * @param clazz the type of class of adapter
     * @param <T>   the returned type
     * @return the @{@link SQLResultAdapter}
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> SQLResultAdapter<T> getAdapter(@NotNull Class<T> clazz) {
        SQLResultAdapter<?> adapter = adapters.get(clazz);
        if (adapter == null) {
            throw new IllegalArgumentException("The adapter for class " + clazz.getSimpleName() + " was not found.");
        }

        return (SQLResultAdapter<T>) adapter;
    }

    /**
     * Register adapters to map queries.
     *
     * @param clazz   the class of adapter
     * @param adapter the @{@link SQLResultAdapter} of clazz
     * @param <T>     the type
     * @return the @{@link SQLExecutor}
     */

    @NotNull
    public <T> SQLExecutor registerAdapter(@NotNull Class<T> clazz, @NotNull SQLResultAdapter<T> adapter) {
        adapters.put(clazz, adapter);
        return this;
    }

    /**
     * Execute a database statement in asynchronous thread
     *
     * @param sql      the sql statement
     * @param consumer the @{@link PreparedStatement} to prepare statement
     * @return the completable future of execution
     *
     * @see #execute(String, StatementConsumer) to execute statement in synchronously
     */
    @Contract("_, _ -> new")
    public @NotNull CompletableFuture<Void> executeAsync(@Language("MySQL") @NotNull String sql,
                                                         @NotNull StatementConsumer consumer) {

        return CompletableFuture.runAsync(() -> execute(sql, consumer), executor);
    }

    /**
     * Execute a database statement.
     *
     * @param sql      the sql statement
     * @param consumer the @{@link PreparedStatement} to prepare statement
     *
     * @see #executeAsync(String, StatementConsumer) to execute statement in asynchronous thread
     */
    public void execute(@Language("MySQL") @NotNull String sql, @NotNull StatementConsumer consumer) {
        sqlConnector.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                consumer.accept(statement);
                statement.execute();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
    }

    /**
     * Execute a database statement in asynchronous thread
     *
     * @param sql the sql statement
     * @return the completable future
     *
     * @see #execute(String) to execute statment in synchronously.
     */
    public CompletableFuture<Void> executeAsync(@Language("MySQL") @NotNull String sql) {
        return CompletableFuture.runAsync(() -> execute(sql), executor);
    }

    /**
     * Execute a database statement.
     *
     * @param sql the sql statement
     * @see #executeAsync(String) to execute statement in asynchronous thread
     */
    public void execute(@Language("MySQL") @NotNull String sql) {
        execute(sql, EMPTY_STATEMENT);
    }

    /**
     * Execute a database query.
     *
     * @param query    the sql query
     * @param consumer the @{@link PreparedStatement} to prepare query
     * @param function the function to map @{@link ResultSet}
     * @param <T>      the returned type
     * @return the optional result of query
     *
     * @see #queryAsync(String, StatementConsumer, ResultSetFunction) to query in asynchronous thread.
     */
    public <T> Optional<T> query(@Language("MySQL") @NotNull String query,
                                 @NotNull StatementConsumer consumer,
                                 @NotNull ResultSetFunction<T> function) {

        AtomicReference<Optional<T>> reference = new AtomicReference<>(Optional.empty());
        sqlConnector.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                consumer.accept(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    reference.set(Optional.ofNullable(function.apply(resultSet)));
                }

            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });

        return reference.get();
    }

    /**
     * Execute a database query.
     *
     * @param query    the sql query
     * @param function the function to map @{@link ResultSet}
     * @param <T>      the returned type
     * @return the optional result of query
     *
     * @see #queryAsync(String, ResultSetFunction) to execute in asynchronous thread.
     */
    public <T> Optional<T> query(@Language("MySQL") @NotNull String query, @NotNull ResultSetFunction<T> function) {
        return query(query, EMPTY_STATEMENT, function);
    }

    /**
     * Select a single entity from a given SQL query
     *
     * @param <T>   the entity type to return.
     * @param query the query to select the entity.
     * @param clazz the class to search @{@link SQLResultAdapter}
     * @return the entity founded, or null
     * @see #queryAsync(String, Class) to execute in asynchronous thread
     */
    public <T> Optional<T> query(@Language("MySQL") @NotNull String query, @NotNull Class<T> clazz) {
        return query(query, EMPTY_STATEMENT, resultSet -> {
            try {
                return resultSet.next() ? getAdapter(clazz).adaptResult(resultSet) : null;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    /**
     * Select a single entity from a given SQL query
     *
     * @param <T>      The entity type to return.
     * @param query    The query to select the entity.
     * @param consumer The statement consumer
     * @param clazz    The class to search adapter
     * @return The entity founded, or null
     *
     * @see #queryAsync(String, StatementConsumer, Class) to execute in asynchronous thread
     */
    public <T> Optional<T> query(@Language("MySQL") @NotNull String query,
                                 @NotNull StatementConsumer consumer,
                                 @NotNull Class<T> clazz
    ) {
        return query(query, consumer, resultSet -> {
            try {
                return resultSet.next() ? getAdapter(clazz).adaptResult(resultSet) : null;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    /**
     * Execute a database query.
     *
     * @param query the sql query
     * @return the optional result of query
     *
     * @see #queryAsync(String) to execute in asynchronous thread.
     */
    public Optional<ResultSet> query(@Language("MySQL") @NotNull String query) {
        return query(query, EMPTY_STATEMENT);
    }

    /**
     * Select a single entity from a given SQL query
     *
     * @param query    the query to select the entity.
     * @param consumer The statement consumer
     * @return the entity founded, or null
     *
     * @see #queryAsync(String, StatementConsumer) to execute in asynchronous thread
     */
    public Optional<ResultSet> query(@Language("MySQL") @NotNull String query, @NotNull StatementConsumer consumer) {
        AtomicReference<Optional<ResultSet>> reference = new AtomicReference<>(Optional.empty());
        sqlConnector.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                consumer.accept(statement);
                reference.set(Optional.ofNullable(statement.executeQuery()));
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
        return reference.get();
    }

    /**
     * Execute a database query in asynchronous thread.
     *
     * @param query    the sql query
     * @param consumer the @{@link PreparedStatement} to prepare query
     * @param function the function to map @{@link ResultSet}
     * @param <T>      the returned type
     * @return the completable future of optional query result
     *
     * @see #query(String, StatementConsumer, ResultSetFunction)  to execute in synchronously
     */
    public <T> CompletableFuture<Optional<T>> queryAsync(@Language("MySQL") @NotNull String query,
                                                         @NotNull StatementConsumer consumer,
                                                         @NotNull ResultSetFunction<T> function
    ) {
        return CompletableFuture.supplyAsync(() -> query(query, consumer, function), executor);
    }

    /**
     * Execute a database query in asynchronous thread.
     *
     * @param query    the sql query
     * @param function the function to map @{@link ResultSet}
     * @param <T>      the returned type
     * @return the completable future of optional query result
     *
     * @see #query(String, ResultSetFunction) to execute in synchronously
     */
    public <T> CompletableFuture<Optional<T>> queryAsync(@Language("MySQL") @NotNull String query,
                                                         @NotNull ResultSetFunction<T> function
    ) {
        return CompletableFuture.supplyAsync(() -> query(query, function), executor);
    }

    /**
     * Execute a database query.
     *
     * @param query    the sql query
     * @param consumer the @{@link ResultSet} to prepare query
     * @param clazz    the class to search @{@link SQLResultAdapter}
     * @param <T>      the returned type
     * @return the completable future of optional query result
     * @see #query(String, StatementConsumer, Class) to execute in synchronously
     */
    public <T> CompletableFuture<Optional<T>> queryAsync(@Language("MySQL") @NotNull String query,
                                                         @NotNull StatementConsumer consumer,
                                                         @NotNull Class<T> clazz
    ) {
        return CompletableFuture.supplyAsync(() -> query(query, consumer, clazz), executor);
    }


    /**
     * Execute a database query.
     *
     * @param query the sql query
     * @param clazz the class to search adapter
     * @param <T>   the returned type
     * @return the completable future of optional query result
     *
     * @see #query(String, Class) to execute in synchronously
     */
    public <T> CompletableFuture<Optional<T>> queryAsync(@Language("MySQL") @NotNull String query, @NotNull Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> query(query, clazz), executor);
    }

    /**
     * Execute a database query.
     *
     * @param query the sql query
     * @return the completable future of optional query result
     *
     * @see #query(String, Class) to execute in synchronously
     */
    public CompletableFuture<Optional<ResultSet>> queryAsync(@Language("MySQL") @NotNull String query) {
        return CompletableFuture.supplyAsync(() -> query(query), executor);
    }

    /**
     * Execute a database query.
     *
     * @param query    the sql query
     * @param consumer the @{@link ResultSet} to prepare query
     * @return the completable future of optional query result
     *
     * @see #query(String, StatementConsumer) to execute in synchronously
     */
    public CompletableFuture<Optional<ResultSet>> queryAsync(@Language("MySQL") @NotNull String query,
                                                             @NotNull StatementConsumer consumer
    ) {
        return CompletableFuture.supplyAsync(() -> query(query, consumer), executor);
    }

    /**
     * Execute a database query
     *
     * @param query    the sql query
     * @param consumer the @{@link PreparedStatement} to prepare query
     * @param clazz    the class to search @{@link SQLResultAdapter}
     * @param <T>      the returned type
     * @return the completable future of @{@link Set} of result
     *
     * @see #queryMany(String, StatementConsumer, Class) to execute in synchronously.
     */
    public <T> CompletableFuture<Set<T>> queryManyAsync(@Language("MySQL") @NotNull String query,
                                                        @NotNull StatementConsumer consumer,
                                                        @NotNull Class<T> clazz) {

        return CompletableFuture.supplyAsync(() -> queryMany(query, consumer, clazz), executor);
    }

    /**
     * Select entities from a given SQL query
     *
     * @param <T>      the entity type to return
     * @param query    the query to select entities
     * @param consumer the statement consumer
     * @param clazz    the class to search @{@link SQLResultAdapter}
     * @return The entities found
     *
     * @see #queryManyAsync(String, StatementConsumer, Class)  to execute in asynchronous thread
     */
    public <T> Set<T> queryMany(@Language("MySQL") @NotNull String query,
                                @NotNull StatementConsumer consumer,
                                @NotNull Class<T> clazz
    ) {
        SQLResultAdapter<T> adapter = getAdapter(clazz);
        return this.query(query, consumer, result -> {

            Set<T> elements = new LinkedHashSet<>();
            while (true) {
                try {
                    if (!result.next()) break;
                    T value = adapter.adaptResult(result);

                    if (value != null) {
                        elements.add(value);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            return elements;

        }).orElse(Collections.emptySet());
    }

    /**
     * Execute a database query
     *
     * @param query the sql query
     * @param clazz the class to search @{@link SQLResultAdapter}
     * @param <T>   the returned type
     * @return the completable future of @{@link Set} of result
     *
     * @see #queryMany(String, Class) to execute in synchronously
     */
    public <T> CompletableFuture<Set<T>> queryManyAsync(@Language("MySQL") @NotNull String query, @NotNull Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> queryMany(query, clazz), executor);
    }

    /**
     * Select entities from a given SQL query
     *
     * @param <T>   the entity type to return
     * @param query the query to select entities
     * @param clazz the class to search @{@link SQLResultAdapter}
     * @return The entities found
     *
     * @see #queryManyAsync(String, Class) to execute in asynchronous thread
     */
    public <T> Set<T> queryMany(@Language("MySQL") @NotNull String query, @NotNull Class<T> clazz) {
        return queryMany(query, EMPTY_STATEMENT, clazz);
    }

    /**
     * Executes a batched database execution.
     *
     * <p>This will be executed on an asynchronous thread.</p>
     *
     * <p>Note that proper implementations of this method should determine
     * if the provided {@link BatchBuilder} is actually worth of being a
     * batched statement. For instance, a BatchBuilder with only one
     * handler can safely be referred to {@link #executeAsync(String, StatementConsumer)}</p>
     *
     * @param builder the builder to be used.
     * @return a Promise of an asynchronous batched database execution
     * @see #executeBatch(BatchBuilder) to perform this action synchronously
     */
    public CompletableFuture<Void> executeBatchAsync(@NotNull BatchBuilder builder) {
        return CompletableFuture.runAsync(() -> this.executeBatch(builder));
    }

    /**
     * Executes a batched database execution.
     *
     * <p>This will be executed on whichever thread it's called from.</p>
     *
     * <p>Note that proper implementations of this method should determine
     * if the provided {@link BatchBuilder} is actually worth of being a
     * batched statement. For instance, a BatchBuilder with only one
     * handler can safely be referred to {@link #execute(String, StatementConsumer)}</p>
     *
     * @param builder the builder to be used.
     *
     * @see #executeBatchAsync(BatchBuilder) to perform this action asynchronously
     */
    public void executeBatch(@NotNull BatchBuilder builder) {
        if (builder.getHandlers().isEmpty()) return;
        if (builder.getHandlers().size() == 1) {
            this.execute(builder.getStatement(), builder.getHandlers().iterator().next());
            return;
        }

        sqlConnector.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(builder.getStatement())) {
                for (StatementConsumer handlers : builder.getHandlers()) {
                    handlers.accept(statement);
                    statement.addBatch();
                }

                statement.executeBatch();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Gets a {@link BatchBuilder} for the provided statement.
     *
     * @param statement the statement to prepare for batching.
     * @return a BatchBuilder
     */
    public BatchBuilder batch(@Language("MySQL") @NotNull String statement) {
        return new BatchBuilder(statement, this);
    }
}
