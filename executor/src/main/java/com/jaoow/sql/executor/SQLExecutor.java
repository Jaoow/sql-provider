package com.jaoow.sql.executor;

import com.jaoow.sql.connector.ConnectorException;
import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.executor.adapter.SQLResultAdapter;
import com.jaoow.sql.executor.batch.BatchBuilder;
import com.jaoow.sql.executor.function.ResultSetConsumer;
import com.jaoow.sql.executor.function.ResultSetFunction;
import com.jaoow.sql.executor.function.StatementConsumer;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class to execute database statements more easily.
 */
@RequiredArgsConstructor
@AllArgsConstructor
public final class SQLExecutor {

    private static final StatementConsumer EMPTY_STATEMENT = statement -> {
    };

    @NotNull private final SQLConnector sqlConnector;
    @NotNull private final Map<Class<?>, SQLResultAdapter<?>> adapters;

    @NotNull private Executor executor = ForkJoinPool.commonPool();

    /**
     * Create an instance of @{@link SQLExecutor}.
     *
     * @param connector the @{@link SQLConnector}.
     */
    public SQLExecutor(@NotNull SQLConnector connector) {
        this.sqlConnector = connector;
        this.adapters = new HashMap<>();
    }

    /**
     * Set the executor to perform asynchronous statements.
     *
     * @param executor tbe @{@link Executor}.
     */
    public void setExecutor(@NotNull Executor executor) {
        this.executor = executor;
    }

    /**
     * This method is used to get the current executor.
     * If there is no transaction associated with the current thread, it returns the default executor.
     * If there is a transaction associated with the current thread, it returns the executor associated with that transaction.
     * If the executor associated with the transaction is null, it throws a NullPointerException with a descriptive message.
     *
     * @return the current executor, either the default one or the one associated with the transaction of the current thread
     * @throws NullPointerException if the executor associated with the transaction of the current thread is null
     */
    public Executor getCurrentExecutor() {
        if (ThreadLocalTransaction.get() == null) {
            return executor;
        }

        final Executor executor = ThreadLocalTransaction.get().getExecutor();

        Objects.requireNonNull(executor, "There is a transaction to the current thread but its executor is null.");

        return executor;
    }

    /**
     * This method is used to get the current SQLConnector.
     * If there is no transaction associated with the current thread, it returns the default SQLConnector.
     * If there is a transaction associated with the current thread, it returns a SQLConnector that uses the connection associated with that transaction.
     * If the connection associated with the transaction is null, it throws a NullPointerException with a descriptive message.
     *
     * @return the current SQLConnector, either the default one or the one associated with the connection of the current thread
     * @throws NullPointerException if the connection associated with the transaction of the current thread is null
     */
    public SQLConnector getCurrentConnection() {
        if (ThreadLocalTransaction.get() == null) {
            return sqlConnector;
        }

        final Connection connection = ThreadLocalTransaction.get().getConnection();

        Objects.requireNonNull(connection, "There is a transaction to the current thread but its connection is null.");

        return consumer -> {
            try {
                consumer.execute(connection);
            } catch (SQLException exception) {
                throw new ConnectorException(exception);
            }
        };
    }

    /**
     * Get the registered @{@link SQLResultAdapter}.
     *
     * @param clazz the type of class of adapter.
     * @param <T>   the returned type.
     * @return the @{@link SQLResultAdapter}.
     */
    @NotNull
    @SuppressWarnings ( "unchecked" )
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
     * @param clazz   the class of adapter.
     * @param adapter the @{@link SQLResultAdapter} of clazz.
     * @param <T>     the type.
     * @return the @{@link SQLExecutor}.
     */
    @NotNull
    public <T> SQLExecutor registerAdapter(@NotNull Class<T> clazz, @NotNull SQLResultAdapter<T> adapter) {
        adapters.put(clazz, adapter);
        return this;
    }

    /**
     * Execute a database statement.
     *
     * @param sql the sql statement.
     * @see #executeAsync(String) to execute statement in asynchronous thread.
     */
    public void execute(@Language ( "MySQL" ) @NotNull String sql) {
        execute(sql, EMPTY_STATEMENT);
    }

    /**
     * Execute a database statement.
     *
     * @param sql     the sql statement.
     * @param prepare the @{@link PreparedStatement} to prepare statement.
     * @see #executeAsync(String, StatementConsumer) to execute statement in asynchronous thread.
     */
    public void execute(@Language ( "MySQL" ) @NotNull String sql, @NotNull StatementConsumer prepare) {
        execute(sql, Statement.NO_GENERATED_KEYS, prepare, EMPTY_STATEMENT);
    }

    /**
     * Execute a database statement and retrieve its result.
     *
     * @param sql    The sql statement.
     * @param result The @{@link ResultSetConsumer} to accept result.
     * @see #executeAsync(String, ResultSetConsumer) to execute statement in asynchronous thread.
     */
    public void execute(@Language ( "MySQL" ) @NotNull String sql, @NotNull ResultSetConsumer result) {
        execute(sql, EMPTY_STATEMENT, result);
    }

    /**
     * Execute a database statement and retrieve its result.
     *
     * @param sql     The sql statement.
     * @param prepare The @{@link PreparedStatement} to prepare statement.
     * @param result  The @{@link ResultSetConsumer} to accept result.
     * @see #executeAsync(String, StatementConsumer, ResultSetConsumer) to execute statement in asynchronous thread.
     */
    public void execute(@Language ( "MySQL" ) @NotNull String sql, @NotNull StatementConsumer prepare, @NotNull ResultSetConsumer result) {
        execute(sql, Statement.RETURN_GENERATED_KEYS, prepare, statement -> {
            try (ResultSet set = statement.getGeneratedKeys()) {
                result.accept(set);
            }
        });
    }

    /**
     * Execute a database statement and retrieve its after execution.
     *
     * @param sql               The sql statement.
     * @param autoGeneratedKeys The flag to indicate if auto generated keys should be retrieved.
     * @param prepare           The @{@link PreparedStatement} to prepare statement.
     * @param result            The @{@link ResultSetConsumer} to accept result.
     * @see #executeAsync(String, int, StatementConsumer, StatementConsumer) to execute statement in asynchronous thread.
     */
    public void execute(@Language ( "MySQL" ) @NotNull String sql, int autoGeneratedKeys,
                        @NotNull StatementConsumer prepare,
                        @NotNull StatementConsumer result) {
        getCurrentConnection().execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, autoGeneratedKeys)) {
                prepare.accept(statement);
                statement.execute();
                result.accept(statement);
            }
        });
    }

    /**
     * Execute a database statement in asynchronous thread.
     *
     * @param sql the sql statement.
     * @return the completable future.
     * @see #execute(String) to execute statment in synchronously.
     */
    @Contract ( "_ -> new" )
    public @NotNull CompletableFuture<Void> executeAsync(@Language ( "MySQL" ) @NotNull String sql) {
        return CompletableFuture.runAsync(() -> execute(sql), getCurrentExecutor());
    }

    /**
     * Execute a database statement in asynchronous thread.
     *
     * @param sql      the sql statement.
     * @param consumer the @{@link PreparedStatement} to prepare statement.
     * @return the completable future of execution.
     * @see #execute(String, StatementConsumer) to execute statement in synchronously.
     */
    @Contract ( "_, _ -> new" )
    public @NotNull CompletableFuture<Void> executeAsync(@Language ( "MySQL" ) @NotNull String sql, @NotNull StatementConsumer consumer) {
        return CompletableFuture.runAsync(() -> execute(sql, consumer), getCurrentExecutor());
    }

    /**
     * Execute a database statement and retrieve its result in an asynchronous thread.
     *
     * @param sql    The sql statement.
     * @param result The @{@link ResultSetConsumer} to accept result.
     * @return the completable future of execution.
     * @see #execute(String, ResultSetConsumer) to execute statement in synchronously.
     */
    public @NotNull CompletableFuture<Void> executeAsync(@Language ( "MySQL" ) @NotNull String sql, @NotNull ResultSetConsumer result) {
        return CompletableFuture.runAsync(() -> execute(sql, result), getCurrentExecutor());
    }

    /**
     * Execute a database statement and retrieve its result in an asynchronous thread.
     *
     * @param sql     The sql statement.
     * @param prepare The @{@link PreparedStatement} to prepare statement.
     * @param result  The @{@link ResultSetConsumer} to accept result.
     * @return the completable future of execution.
     * @see #execute(String, StatementConsumer, ResultSetConsumer) to execute statement in synchronously.
     */
    public @NotNull CompletableFuture<Void> executeAsync(@Language ( "MySQL" ) @NotNull String sql,
                                                         @NotNull StatementConsumer prepare,
                                                         @NotNull ResultSetConsumer result) {

        return CompletableFuture.runAsync(() -> execute(sql, prepare, result), getCurrentExecutor());
    }

    /**
     * Execute a database statement and retrieve after its execution in an asynchronous thread.
     *
     * @param sql               The sql statement.
     * @param autoGeneratedKeys The flag to indicate if auto generated keys should be retrieved.
     * @param prepare           The @{@link PreparedStatement} to prepare statement.
     * @param result            The @{@link ResultSetConsumer} to accept result.
     * @return the completable future of execution
     * @see #execute(String, int, StatementConsumer, StatementConsumer) to execute statement in synchronously.
     */
    public @NotNull CompletableFuture<Void> executeAsync(@Language ( "MySQL" ) @NotNull String sql,
                                                         int autoGeneratedKeys,
                                                         @NotNull StatementConsumer prepare,
                                                         @NotNull StatementConsumer result) {

        return CompletableFuture.runAsync(() -> execute(sql, autoGeneratedKeys, prepare, result), getCurrentExecutor());
    }

    /**
     * Execute a database query.
     *
     * @param query    the sql query
     * @param consumer the @{@link PreparedStatement} to prepare query
     * @param function the function to map @{@link ResultSet}
     * @param <T>      the returned type
     * @return the optional result of query
     * @see #queryAsync(String, StatementConsumer, ResultSetFunction) to query in asynchronous.
     */
    public <T> Optional<T> query(@Language ( "MySQL" ) @NotNull String query,
                                 @NotNull StatementConsumer consumer,
                                 @NotNull ResultSetFunction<T> function) {

        AtomicReference<Optional<T>> reference = new AtomicReference<>(Optional.empty());
        getCurrentConnection().execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                consumer.accept(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    reference.set(Optional.ofNullable(function.apply(resultSet)));
                }
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
     * @see #queryAsync(String, ResultSetFunction) to execute in asynchronous thread.
     */
    public <T> Optional<T> query(@Language ( "MySQL" ) @NotNull String query, @NotNull ResultSetFunction<T> function) {
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
    public <T> Optional<T> query(@Language ( "MySQL" ) @NotNull String query, @NotNull Class<T> clazz) {
        return query(query, EMPTY_STATEMENT, resultSet -> resultSet.next() ? getAdapter(clazz).adaptResult(resultSet) : null);
    }

    /**
     * Select a single entity from a given SQL query
     *
     * @param <T>      The entity type to return.
     * @param query    The query to select the entity.
     * @param consumer The statement consumer
     * @param clazz    The class to search adapter
     * @return The entity founded, or null
     * @see #queryAsync(String, StatementConsumer, Class) to execute in asynchronous thread
     */
    public <T> Optional<T> query(@Language ( "MySQL" ) @NotNull String query,
                                 @NotNull StatementConsumer consumer,
                                 @NotNull Class<T> clazz
    ) {
        return query(query, consumer, resultSet -> resultSet.next() ? getAdapter(clazz).adaptResult(resultSet) : null);
    }

    /**
     * Execute a database query.
     *
     * @param query the sql query
     * @return the optional result of query
     * @see #queryAsync(String) to execute in asynchronous thread.
     */
    public Optional<ResultSet> query(@Language ( "MySQL" ) @NotNull String query) {
        return query(query, EMPTY_STATEMENT);
    }

    /**
     * Select a single entity from a given SQL query
     *
     * @param query    the query to select the entity.
     * @param consumer The statement consumer
     * @return the entity founded, or null
     * @see #queryAsync(String, StatementConsumer) to execute in asynchronous thread
     */
    public Optional<ResultSet> query(@Language ( "MySQL" ) @NotNull String query, @NotNull StatementConsumer consumer) {
        AtomicReference<Optional<ResultSet>> reference = new AtomicReference<>(Optional.empty());
        getCurrentConnection().execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                consumer.accept(statement);
                reference.set(Optional.ofNullable(statement.executeQuery()));
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
     * @see #query(String, StatementConsumer, ResultSetFunction)  to execute in synchronously
     */
    @Contract ( "_, _, _ -> new" )
    public <T> @NotNull CompletableFuture<Optional<T>> queryAsync(@Language ( "MySQL" ) @NotNull String query,
                                                                  @NotNull StatementConsumer consumer,
                                                                  @NotNull ResultSetFunction<T> function
    ) {
        return CompletableFuture.supplyAsync(() -> query(query, consumer, function), getCurrentExecutor());
    }

    /**
     * Execute a database query in asynchronous thread.
     *
     * @param query    the sql query
     * @param function the function to map @{@link ResultSet}
     * @param <T>      the returned type
     * @return the completable future of optional query result
     * @see #query(String, ResultSetFunction) to execute in synchronously
     */
    @Contract ( "_, _ -> new" )
    public <T> @NotNull CompletableFuture<Optional<T>> queryAsync(@Language ( "MySQL" ) @NotNull String query,
                                                                  @NotNull ResultSetFunction<T> function
    ) {
        return CompletableFuture.supplyAsync(() -> query(query, function), getCurrentExecutor());
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
    @Contract ( "_, _, _ -> new" )
    public <T> @NotNull CompletableFuture<Optional<T>> queryAsync(@Language ( "MySQL" ) @NotNull String query,
                                                                  @NotNull StatementConsumer consumer,
                                                                  @NotNull Class<T> clazz
    ) {
        return CompletableFuture.supplyAsync(() -> query(query, consumer, clazz), getCurrentExecutor());
    }


    /**
     * Execute a database query.
     *
     * @param query the sql query
     * @param clazz the class to search adapter
     * @param <T>   the returned type
     * @return the completable future of optional query result
     * @see #query(String, Class) to execute in synchronously
     */
    @Contract ( "_, _ -> new" )
    public <T> @NotNull CompletableFuture<Optional<T>> queryAsync(@Language ( "MySQL" ) @NotNull String query, @NotNull Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> query(query, clazz), getCurrentExecutor());
    }

    /**
     * Execute a database query.
     *
     * @param query the sql query
     * @return the completable future of optional query result
     * @see #query(String, Class) to execute in synchronously
     */
    @Contract ( "_ -> new" )
    public @NotNull CompletableFuture<Optional<ResultSet>> queryAsync(@Language ( "MySQL" ) @NotNull String query) {
        return CompletableFuture.supplyAsync(() -> query(query), getCurrentExecutor());
    }

    /**
     * Execute a database query.
     *
     * @param query    the sql query
     * @param consumer the @{@link ResultSet} to prepare query
     * @return the completable future of optional query result
     * @see #query(String, StatementConsumer) to execute in synchronously
     */
    @Contract ( "_, _ -> new" )
    public @NotNull CompletableFuture<Optional<ResultSet>> queryAsync(@Language ( "MySQL" ) @NotNull String query,
                                                                      @NotNull StatementConsumer consumer
    ) {
        return CompletableFuture.supplyAsync(() -> query(query, consumer), getCurrentExecutor());
    }

    /**
     * Execute a database query
     *
     * @param query    the sql query
     * @param consumer the @{@link PreparedStatement} to prepare query
     * @param clazz    the class to search @{@link SQLResultAdapter}
     * @param <T>      the returned type
     * @return the completable future of @{@link Set} of result
     * @see #queryMany(String, StatementConsumer, Class) to execute in synchronously.
     */
    @Contract ( "_, _, _ -> new" )
    public <T> @NotNull CompletableFuture<Set<T>> queryManyAsync(@Language ( "MySQL" ) @NotNull String query,
                                                                 @NotNull StatementConsumer consumer,
                                                                 @NotNull Class<T> clazz) {

        return CompletableFuture.supplyAsync(() -> queryMany(query, consumer, clazz), getCurrentExecutor());
    }

    /**
     * Select entities from a given SQL query
     *
     * @param <T>      the entity type to return
     * @param query    the query to select entities
     * @param consumer the statement consumer
     * @param clazz    the class to search @{@link SQLResultAdapter}
     * @return The entities found
     * @see #queryManyAsync(String, StatementConsumer, Class)  to execute in asynchronous thread
     */
    public <T> Set<T> queryMany(@Language ( "MySQL" ) @NotNull String query,
                                @NotNull StatementConsumer consumer,
                                @NotNull Class<T> clazz
    ) {
        SQLResultAdapter<T> adapter = getAdapter(clazz);
        return this.query(query, consumer, result -> {

            Set<T> elements = new LinkedHashSet<>();
            while (result.next()) {
                T value = adapter.adaptResult(result);
                if (value != null)
                    elements.add(value);
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
     * @see #queryMany(String, Class) to execute in synchronously
     */
    @Contract ( "_, _ -> new" )
    public <T> @NotNull CompletableFuture<Set<T>> queryManyAsync(@Language ( "MySQL" ) @NotNull String query, @NotNull Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> queryMany(query, clazz), getCurrentExecutor());
    }

    /**
     * Select entities from a given SQL query
     *
     * @param <T>   the entity type to return
     * @param query the query to select entities
     * @param clazz the class to search @{@link SQLResultAdapter}
     * @return The entities found
     * @see #queryManyAsync(String, Class) to execute in asynchronous thread
     */
    public <T> Set<T> queryMany(@Language ( "MySQL" ) @NotNull String query, @NotNull Class<T> clazz) {
        return queryMany(query, EMPTY_STATEMENT, clazz);
    }


    /**
     * Gets a {@link BatchBuilder} for the provided statement.
     *
     * @param statement the statement to prepare for batching.
     * @return a BatchBuilder
     */
    @Contract ( "_ -> new" )
    public @NotNull BatchBuilder batch(@Language ( "MySQL" ) @NotNull String statement) {
        return new BatchBuilder(statement, this);
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
     * @see #executeBatchAsync(BatchBuilder) to perform this action asynchronously.
     */
    public void executeBatch(@NotNull BatchBuilder builder) {
        executeBatch(builder, Statement.NO_GENERATED_KEYS, EMPTY_STATEMENT);
    }

    /**
     * Executes a batched database execution and retrieve statement.
     *
     * <p>This will be executed on whichever thread it's called from.</p>
     *
     * <p>Note that proper implementations of this method should determine
     * if the provided {@link BatchBuilder} is actually worth of being a
     * batched statement. For instance, a BatchBuilder with only one
     * handler can safely be referred to {@link #execute(String, StatementConsumer)}</p>
     *
     * @param builder The builder to be used.
     * @param result  The result to be accepted.
     * @see #executeBatchAsync(BatchBuilder, ResultSetConsumer) to perform this action asynchronously.
     */
    public void executeBatch(@NotNull BatchBuilder builder, ResultSetConsumer result) {
        executeBatch(builder, Statement.RETURN_GENERATED_KEYS, statement -> {
            try (ResultSet set = statement.getGeneratedKeys()) {
                result.accept(set);
            }
        });
    }

    /**
     * Executes a batched database execution and retrieve statement.
     *
     * <p>This will be executed on whichever thread it's called from.</p>
     *
     * <p>Note that proper implementations of this method should determine
     * if the provided {@link BatchBuilder} is actually worth of being a
     * batched statement. For instance, a BatchBuilder with only one
     * handler can safely be referred to {@link #execute(String, StatementConsumer)}</p>
     *
     * @param builder           The builder to be used.
     * @param autoGeneratedKeys The flag to indicate if auto generated keys should be retrieved.
     * @param result            The result to be accepted.
     * @see #executeBatchAsync(BatchBuilder, int, StatementConsumer) to perform this action asynchronously.
     */
    public void executeBatch(@NotNull BatchBuilder builder, int autoGeneratedKeys, @NotNull StatementConsumer result) {
        if (builder.getHandlers().isEmpty()) return;
        if (builder.getHandlers().size() == 1) {
            this.execute(builder.getStatement(), autoGeneratedKeys, builder.getHandlers().iterator().next(), result);
            return;
        }

        getCurrentConnection().execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(builder.getStatement(), autoGeneratedKeys)) {
                for (StatementConsumer handlers : builder.getHandlers()) {
                    handlers.accept(statement);
                    statement.addBatch();
                }

                statement.executeBatch();
                result.accept(statement);
            }
        });
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
    @Contract ( "_ -> new" )
    public @NotNull CompletableFuture<Void> executeBatchAsync(@NotNull BatchBuilder builder) {
        return CompletableFuture.runAsync(() -> this.executeBatch(builder));
    }

    /**
     * Executes a batched database execution and retrieve statement.
     *
     * <p>This will be executed on an asynchronous thread.</p>
     *
     * <p>Note that proper implementations of this method should determine
     * if the provided {@link BatchBuilder} is actually worth of being a
     * batched statement. For instance, a BatchBuilder with only one
     * handler can safely be referred to {@link #executeAsync(String, StatementConsumer, ResultSetConsumer)}</p>
     *
     * @param builder The builder to be used.
     * @param result  The result to be accepted.
     * @return a Promise of an asynchronous batched database execution
     * @see #executeBatch(BatchBuilder, ResultSetConsumer) to perform this action synchronously
     */
    @Contract ( "_,_ -> new" )
    public @NotNull CompletableFuture<Void> executeBatchAsync(@NotNull BatchBuilder builder, @NotNull ResultSetConsumer result) {
        return CompletableFuture.runAsync(() -> this.executeBatch(builder, result));
    }

    /**
     * Executes a batched database execution and retrieve statement.
     *
     * <p>This will be executed on an asynchronous thread.</p>
     *
     * <p>Note that proper implementations of this method should determine
     * if the provided {@link BatchBuilder} is actually worth of being a
     * batched statement. For instance, a BatchBuilder with only one
     * handler can safely be referred to {@link #executeAsync(String, int, StatementConsumer, StatementConsumer)}</p>
     *
     * @param builder           The builder to be used.
     * @param autoGeneratedKeys The flag to indicate if auto generated keys should be retrieved.
     * @param result            The result to be accepted.
     * @return a Promise of an asynchronous batched database execution
     * @see #executeBatch(BatchBuilder, int, StatementConsumer) to perform this action synchronously
     */
    @Contract ( "_,_,_ -> new" )
    public @NotNull CompletableFuture<Void> executeBatchAsync(@NotNull BatchBuilder builder, int autoGeneratedKeys, @NotNull StatementConsumer result) {
        return CompletableFuture.runAsync(() -> this.executeBatch(builder, autoGeneratedKeys, result));
    }

    /**
     * Any query to the database using any method from SQLExecutor (like execute, executeAsync, query, queryAsync, etc..) will be executed inside a transaction (will reuse the same connection).
     * If any exception is thrown during the execution of the runnable, it rolls back the transaction and rethrows the exception as a RuntimeException.
     * All code inside runnable  will be executed asynchronously using the default executor.
     *
     * @param runnable the Runnable to be executed within the transaction
     * @return a CompletableFuture that will be completed when the transaction and the execution of the runnable are finished
     */
    public CompletableFuture<Void> withTransaction(Runnable runnable) {
        return CompletableFuture.runAsync(() -> {
            sqlConnector.execute(connection -> {
                connection.setAutoCommit(false);

                final SQLTransactionHolder transactionHolder = new SQLTransactionHolder(connection, new CurrentThreadExecutor());
                ThreadLocalTransaction.set(transactionHolder);

                try {
                    runnable.run();
                    connection.commit();
                } catch (Throwable t) {
                    connection.rollback();
                    throw new RuntimeException(t);
                } finally {
                    ThreadLocalTransaction.remove();
                }
            });
        }, executor);
    }

}
