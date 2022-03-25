package com.jaoow.sql.executor;

import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.executor.adapter.SQLResultAdapter;
import com.jaoow.sql.executor.batch.BatchBuilder;
import com.jaoow.sql.executor.exceptions.SQLAdapterNotFoundException;
import com.jaoow.sql.executor.result.SimpleResultSet;
import com.jaoow.sql.executor.statement.SimpleStatement;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Class to execute database statements more easily
 */
@RequiredArgsConstructor
@AllArgsConstructor
public final class SQLExecutor {

    private static final Consumer<SimpleStatement> EMPTY_STATEMENT = statement -> {};

    @NotNull private final SQLConnector sqlConnector;
    @NotNull private final Map<Class<?>, SQLResultAdapter<?>> adapterMap;

    @NotNull
    private Executor executor = ForkJoinPool.commonPool();

    /**
     * Create an instance of @{@link SQLExecutor}
     *
     * @param connector the @{@link SQLConnector}
     */
    public SQLExecutor(@NotNull SQLConnector connector) {
        this.sqlConnector = connector;
        this.adapterMap = new HashMap<>();
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
    @Nullable
    public <T> SQLResultAdapter<T> getAdapter(@NotNull Class<T> clazz) {
        SQLResultAdapter<?> adapter = adapterMap.get(clazz);
        return adapter == null ? null : (SQLResultAdapter<T>) adapter;
    }

    /**
     * Execute a database statement in asynchronous thread
     *
     * @param sql      the sql statement
     * @param consumer the @{@link SimpleStatement} to prepare statement
     * @return the completable future of execution
     * @see #execute(String, Consumer) to execute statement in synchronously
     */
    public CompletableFuture<Void> executeAsync(@Language("MySQL") @NotNull String sql,
                                                @NotNull Consumer<SimpleStatement> consumer
    ) {
        return CompletableFuture.runAsync(() -> execute(sql, consumer), executor);
    }

    /**
     * Execute a database statement.
     *
     * @param sql      the sql statement
     * @param consumer the @{@link SimpleStatement} to prepare statement
     * @see #executeAsync(String, Consumer) to execute statement in asynchronous thread
     */
    public void execute(@Language("MySQL") @NotNull String sql, @NotNull Consumer<SimpleStatement> consumer) {
        sqlConnector.execute(connection -> {
            try (SimpleStatement statement = SimpleStatement.of(connection.prepareStatement(sql))) {
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
     * Execute a database query in asynchronous thread.
     *
     * @param query    the sql query
     * @param consumer the @{@link SimpleStatement} to prepare query
     * @param function the function to map @{@link SimpleResultSet}
     * @param <T>      the returned type
     * @return the completable future of optional query result
     * @see #query(String, Consumer, Function)  to execute in synchronously
     */
    public <T> CompletableFuture<Optional<T>> queryAsync(@Language("MySQL") @NotNull String query,
                                                         @NotNull Consumer<SimpleStatement> consumer,
                                                         @NotNull Function<SimpleResultSet, T> function
    ) {
        return CompletableFuture.supplyAsync(() -> query(query, consumer, function), executor);
    }

    /**
     * Execute a database query.
     *
     * @param query    the sql query
     * @param consumer the @{@link SimpleStatement} to prepare query
     * @param function the function to map @{@link SimpleResultSet}
     * @param <T>      the returned type
     * @return the optional result of query
     * @see #queryAsync(String, Consumer, Function) to query in asynchronous thread.
     */
    public <T> Optional<T> query(@Language("MySQL") @NotNull String query,
                                 @NotNull Consumer<SimpleStatement> consumer,
                                 @NotNull Function<SimpleResultSet, T> function
    ) {
        AtomicReference<Optional<T>> reference = new AtomicReference<>(Optional.empty());
        sqlConnector.execute(connection -> {
            try (SimpleStatement statement = SimpleStatement.of(connection.prepareStatement(query))) {
                consumer.accept(statement);
                try (SimpleResultSet resultSet = statement.executeQuery()) {
                    reference.set(Optional.ofNullable(function.apply(resultSet)));
                }
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
     * @param function the function to map @{@link SimpleResultSet}
     * @param <T>      the returned type
     * @return the completable future of optional query result
     * @see #query(String, Function) to execute in synchronously
     */
    public <T> CompletableFuture<Optional<T>> queryAsync(@Language("MySQL") @NotNull String query,
                                                         @NotNull Function<SimpleResultSet, T> function
    ) {
        return CompletableFuture.supplyAsync(() -> query(query, function), executor);
    }

    /**
     * Execute a database query.
     *
     * @param query    the sql query
     * @param function the function to map @{@link SimpleResultSet}
     * @param <T>      the returned type
     * @return the optional result of query
     * @see #queryAsync(String, Function) to execute in asynchronous thread.
     */
    public <T> Optional<T> query(@Language("MySQL") @NotNull String query, @NotNull Function<SimpleResultSet, T> function) {
        return query(query, EMPTY_STATEMENT, function);
    }

    /**
     * Execute a database query.
     *
     * @param query    the sql query
     * @param consumer the @{@link SimpleResultSet} to prepare query
     * @param clazz    the class to search @{@link SQLResultAdapter}
     * @param <T>      the returned type
     * @return the completable future of optional query result
     * @throws SQLAdapterNotFoundException if {@link SQLResultAdapter} is null
     * @see #query(String, Consumer, Class) to execute in synchronously
     */
    public <T> CompletableFuture<Optional<T>> queryAsync(@Language("MySQL") @NotNull String query,
                                                         @NotNull Consumer<SimpleStatement> consumer,
                                                         @NotNull Class<T> clazz
    ) {
        return CompletableFuture.supplyAsync(() -> query(query, consumer, clazz), executor);
    }

    /**
     * Select a single entity from a given SQL query
     *
     * @param <T>      The entity type to return.
     * @param query    The query to select the entity.
     * @param consumer The statement consumer
     * @param clazz    The class to search adapter
     * @return The entity founded, or null
     * @throws SQLAdapterNotFoundException if {@link SQLResultAdapter} is null
     * @see #queryAsync(String, Consumer, Class) to execute in asynchronous thread
     */
    public <T> Optional<T> query(@Language("MySQL") @NotNull String query,
                                 @NotNull Consumer<SimpleStatement> consumer,
                                 @NotNull Class<T> clazz
    ) {
        SQLResultAdapter<T> adapter = getAdapter(clazz);
        if (adapter == null) {
            throw new SQLAdapterNotFoundException(clazz);
        }

        return query(query, consumer, resultSet -> resultSet.next() ? adapter.adaptResult(resultSet) : null);
    }

    /**
     * Execute a database query.
     *
     * @param query the sql query
     * @param clazz the class to search adapter
     * @param <T>   the returned type
     * @return the completable future of optional query result
     * @throws SQLAdapterNotFoundException if {@link SQLResultAdapter} is null
     * @see #query(String, Class) to execute in synchronously
     */
    public <T> CompletableFuture<Optional<T>> queryAsync(@Language("MySQL") @NotNull String query, @NotNull Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> query(query, clazz), executor);
    }

    /**
     * Select a single entity from a given SQL query
     *
     * @param <T>   the entity type to return.
     * @param query the query to select the entity.
     * @param clazz the class to search @{@link SQLResultAdapter}
     * @return the entity founded, or null
     * @throws SQLAdapterNotFoundException if {@link SQLResultAdapter} is null
     * @see #queryAsync(String, Class) to execute in asynchronous thread
     */
    public <T> Optional<T> query(@Language("MySQL") @NotNull String query, @NotNull Class<T> clazz) {
        SQLResultAdapter<T> adapter = getAdapter(clazz);
        if (adapter == null) {
            throw new SQLAdapterNotFoundException(clazz);
        }

        return query(query, EMPTY_STATEMENT, resultSet -> resultSet.next() ? adapter.adaptResult(resultSet) : null);
    }

    /**
     * Execute a database query
     *
     * @param query    the sql query
     * @param consumer the @{@link SimpleStatement} to prepare query
     * @param clazz    the class to search @{@link SQLResultAdapter}
     * @param <T>      the returned type
     * @return the completable future of @{@link Set} of result
     * @throws SQLAdapterNotFoundException if {@link SQLResultAdapter} is null
     * @see #queryMany(String, Consumer, Class) to execute in synchronously.
     */
    public <T> CompletableFuture<Set<T>> queryManyAsync(@Language("MySQL") @NotNull String query,
                                                        @NotNull Consumer<SimpleStatement> consumer,
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
     * @throws SQLAdapterNotFoundException if {@link SQLResultAdapter} is null
     * @see #queryManyAsync(String, Consumer, Class)  to execute in asynchronous thread
     */
    public <T> Set<T> queryMany(@Language("MySQL") @NotNull String query,
                                @NotNull Consumer<SimpleStatement> consumer,
                                @NotNull Class<T> clazz
    ) {
        SQLResultAdapter<T> adapter = getAdapter(clazz);
        if (adapter == null) {
            throw new SQLAdapterNotFoundException(clazz);
        }

        return this.query(query, consumer, result -> {

            Set<T> elements = new LinkedHashSet<>();
            while (result.next()) {
                T value = adapter.adaptResult(result);
                if (value != null) {
                    elements.add(value);
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
     * @throws SQLAdapterNotFoundException if {@link SQLResultAdapter} is null
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
     * @throws SQLAdapterNotFoundException if {@link SQLResultAdapter} is null
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
     * handler can safely be referred to {@link #executeAsync(String, Consumer)}</p>
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
     * handler can safely be referred to {@link #execute(String, Consumer)}</p>
     *
     * @param builder the builder to be used.
     * @see #executeBatchAsync(BatchBuilder) to perform this action asynchronously
     */
    public void executeBatch(@NotNull BatchBuilder builder) {

        if (builder.getHandlers().isEmpty()) return;
        if (builder.getHandlers().size() == 1) {
            this.execute(builder.getStatement(), builder.getHandlers().iterator().next());
            return;
        }

        sqlConnector.execute(connection -> {
            try (SimpleStatement statement = SimpleStatement.of(connection.prepareStatement(builder.getStatement()))) {

                for (Consumer<SimpleStatement> handlers : builder.getHandlers()) {
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
