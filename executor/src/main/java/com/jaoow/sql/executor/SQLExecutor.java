package com.jaoow.sql.executor;

import com.jaoow.sql.connector.exception.ConnectorException;
import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.executor.adapter.SQLResultAdapter;
import com.jaoow.sql.executor.batch.BatchBuilder;
import com.jaoow.sql.executor.exception.ExecutorException;
import com.jaoow.sql.executor.function.ResultSetConsumer;
import com.jaoow.sql.executor.function.ResultSetFunction;
import com.jaoow.sql.executor.function.StatementConsumer;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Class to execute database statements more easily.
 */
public final class SQLExecutor {

    @NotNull private final SQLConnector sqlConnector;
    @NotNull private final Map<Class<?>, SQLResultAdapter<?>> adapters;

    @NotNull private Executor executor = ForkJoinPool.commonPool();

    /**
     * Construct a sql executor with given {@link SQLConnector}
     *
     * @param connector the sql connector.
     */
    public SQLExecutor(@NotNull SQLConnector connector) {
        this.sqlConnector = Objects.requireNonNull(connector, "SQLConnector cannot be null");
        this.adapters = new HashMap<>();
    }

    /**
     * Construct a sql executor with given {@link SQLConnector} and map of {@link SQLResultAdapter}.
     *
     * @param sqlConnector The sql connector.
     * @param adapters THe map of sql adapters.
     */
    public SQLExecutor(@NotNull SQLConnector sqlConnector, @NotNull Map<Class<?>, SQLResultAdapter<?>> adapters) {
        this.sqlConnector = Objects.requireNonNull(sqlConnector, "SQLConnector cannot be null");
        this.adapters = Objects.requireNonNull(adapters, "Adapters cannot be null");
    }

    /**
     * Construct a sql executor with given {@link SQLConnector}, map of {@link SQLResultAdapter} and {@link Executor}.
     *
     * @param sqlConnector The sql connector.
     * @param adapters The map of sql adapters.
     * @param executor The executor.
     */
    public SQLExecutor(@NotNull SQLConnector sqlConnector, @NotNull Map<Class<?>, SQLResultAdapter<?>> adapters, @NotNull Executor executor) {
        this.sqlConnector = Objects.requireNonNull(sqlConnector, "SQLConnector cannot be null");
        this.adapters = Objects.requireNonNull(adapters, "Adapters cannot be null");
        this.executor = Objects.requireNonNull(executor, "Executor cannot be null");
    }

    /**
     * Set the @{@link Executor} to perform asynchronous statements.
     *
     * @param executor tbe executor.
     */
    public void setExecutor(@NotNull Executor executor) {
        this.executor = Objects.requireNonNull(executor, "Executor cannot be null");
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
    @NotNull
    public Executor getCurrentExecutor() {
        SQLTransactionHolder holder = ThreadLocalTransaction.get();
        if (holder == null) return this.executor;

        Executor executor = holder.getExecutor();
        return Objects.requireNonNull(executor, "There is a transaction to the current thread but its executor is null.");
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
    @NotNull
    public SQLConnector getCurrentConnection() {
        SQLTransactionHolder holder = ThreadLocalTransaction.get();
        if (holder == null) return this.sqlConnector;

        Connection connection = holder.getConnection();
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
     * @param clazz The adapter mapper class.
     * @param <T> The class type.
     * @return the adapter.
     * @throws IllegalArgumentException if the adapter for the class was not found.
     */
    @NotNull
    @SuppressWarnings ("unchecked")
    public <T> SQLResultAdapter<T> getAdapter(@NotNull Class<T> clazz) {
        SQLResultAdapter<?> adapter = adapters.get(Objects.requireNonNull(clazz, "Class cannot be null"));
        if (adapter == null) {
            throw new IllegalArgumentException("The adapter for class " + clazz.getSimpleName() + " was not found.");
        }

        return (SQLResultAdapter<T>) adapter;
    }

    /**
     * Register an @{@link SQLResultAdapter} to map queries.
     *
     * @param clazz The adapter mapper class.
     * @param adapter The adapter.
     * @param <T> The adapter type.
     * @return this executor.
     */
    @NotNull
    @Contract("_, _ -> this")
    public <T> SQLExecutor registerAdapter(@NotNull Class<T> clazz, @NotNull SQLResultAdapter<T> adapter) {
        adapters.put(
                Objects.requireNonNull(clazz, "Class cannot be null"),
                Objects.requireNonNull(adapter, "Adapter cannot be null"));
        return this;
    }

    /**
     * Execute a database statement.
     *
     * @param sql the sql statement.
     * @see #executeAsync(String) to execute statement in asynchronous thread.
     */
    public void execute(@Language ( "MySQL" ) @NotNull String sql) {
        execute(sql, StatementConsumer.EMPTY_STATEMENT);
    }

    /**
     * Execute a database statement prepared with @{@link StatementConsumer}
     *
     * @param sql The sql statement.
     * @param prepare The statement consumer.
     * @see #executeAsync(String, StatementConsumer) to execute statement in asynchronous thread.
     */
    public void execute(@Language ( "MySQL" ) @NotNull String sql, @NotNull StatementConsumer prepare) {
        execute(sql, Statement.NO_GENERATED_KEYS, prepare, StatementConsumer.EMPTY_STATEMENT);
    }

    /**
     * Execute a database statement and retrieve its result on @{@link ResultSetConsumer}
     *
     * @param sql The sql statement.
     * @param result The result set consumer to accept results.
     * @see #executeAsync(String, ResultSetConsumer) to execute statement in asynchronous thread.
     */
    public void execute(@Language ( "MySQL" ) @NotNull String sql, @NotNull ResultSetConsumer result) {
        execute(sql, StatementConsumer.EMPTY_STATEMENT, result);
    }

    /**
     * Execute a database statement prepared with @{@link StatementConsumer} and retrieve its result on @{@link ResultSetConsumer}
     *
     * @param sql The sql statement.
     * @param prepare The statement consumer to prepare statement.
     * @param result The result set consumer to accept results.
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
     * Execute a database statement with auto generated keys flags and retrieve its result
     * on @{@link ResultSetConsumer} to execute another statement.
     *
     * @param sql The sql statement.
     * @param autoGeneratedKeys The flag to indicate if auto generated keys should be retrieved.
     * @param prepare The consumer to prepare statement.
     * @param result The consumer to accept results.
     * @see #executeAsync(String, int, StatementConsumer, StatementConsumer) to execute statement in asynchronous thread.
     */
    public void execute(@Language ( "MySQL" ) @NotNull String sql,
                        int autoGeneratedKeys,
                        @NotNull StatementConsumer prepare,
                        @NotNull StatementConsumer result) {

        Objects.requireNonNull(sql, "SQL statement cannot be null");
        Objects.requireNonNull(prepare, "Prepare statement cannot be null");
        Objects.requireNonNull(result, "Result statement cannot be null");

        getCurrentConnection().execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, autoGeneratedKeys)) {
                prepare.accept(statement);
                statement.execute();
                result.accept(statement);
            }
        });
    }

    /**
     * Execute asynchronous database statement.
     *
     * @param sql the sql statement.
     * @return The completable future of execution.
     * @see #execute(String) to execute statment in synchronously.
     */
    @NotNull
    @Contract ( "_ -> new" )
    public CompletableFuture<Void> executeAsync(@Language("MySQL") @NotNull String sql) {
        return runAsync(() -> execute(sql));
    }

    /**
     * Execute asynchronous database statement prepared with @{@link StatementConsumer}.
     *
     * @param sql The sql statement.
     * @param consumer the consumer to prepare statement.
     * @return The completable future of execution.
     * @see #execute(String, StatementConsumer) to execute statement in synchronously.
     */
    @NotNull
    @Contract ( "_, _ -> new" )
    public CompletableFuture<Void> executeAsync(@Language ( "MySQL" ) @NotNull String sql, @NotNull StatementConsumer consumer) {
        return runAsync(() -> execute(sql, consumer));
    }

    /**
     * Execute asynchronous database statement and retrieve its result on @{@link ResultSetConsumer}.
     *
     * @param sql The sql statement.
     * @param result The result set consumer to accept results.
     * @return the completable future of execution.
     * @see #execute(String, ResultSetConsumer) to execute statement in synchronously.
     */
    public @NotNull CompletableFuture<Void> executeAsync(@Language ( "MySQL" ) @NotNull String sql, @NotNull ResultSetConsumer result) {
        return runAsync(() -> execute(sql, result));
    }

    /**
     * Execute asynchronous database statement prepared with @{@link StatementConsumer} and retrieve its result in @{@link ResultSetConsumer}
     *
     * @param sql The sql statement.
     * @param prepare The consumer to prepare statement.
     * @param result The result set consumer to accept results.
     * @return the completable future of execution.
     * @see #execute(String, StatementConsumer, ResultSetConsumer) to execute statement in synchronously.
     */
    public @NotNull CompletableFuture<Void> executeAsync(@Language ( "MySQL" ) @NotNull String sql,
                                                         @NotNull StatementConsumer prepare,
                                                         @NotNull ResultSetConsumer result) {

        return runAsync(() -> execute(sql, prepare, result));
    }

    /**
     * Execute asynchronous database statement with auto generated keys flags and retrieve its result
     * on @{@link ResultSetConsumer} to execute another statement.
     *
     * @param sql The sql statement.
     * @param autoGeneratedKeys The flag to indicate if auto generated keys should be retrieved.
     * @param prepare The consumer to prepare statement.
     * @param result The consumer to accept results.
     * @return the completable future of execution.
     * @see #execute(String, int, StatementConsumer, StatementConsumer) to execute statement in synchronous.
     */
    @NotNull
    public CompletableFuture<Void> executeAsync(@Language("MySQL") @NotNull String sql,
                                                int autoGeneratedKeys,
                                                @NotNull StatementConsumer prepare,
                                                @NotNull StatementConsumer result) {
        return runAsync(() -> execute(sql, autoGeneratedKeys, prepare, result));
    }

    /**
     * Execute a database statement prepared with @{@link StatementConsumer} and map their results with @{@link ResultSetFunction}.
     *
     * @param query The sql query
     * @param consumer The statement consumer to prepare statement.
     * @param function The result function to map results.
     * @param <T> The returned type.
     * @return The optional result.
     * @see #queryAsync(String, StatementConsumer, ResultSetFunction) to query in asynchronous.
     */
    public <T> Optional<T> query(@Language("MySQL") @NotNull String query,
                                 @NotNull StatementConsumer consumer,
                                 @NotNull ResultSetFunction<T> function) {

        Objects.requireNonNull(query, "SQL query cannot be null");
        Objects.requireNonNull(consumer, "Statement consumer cannot be null");
        Objects.requireNonNull(function, "Result function cannot be null");

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
     * Execute a database statement and map their result with @{@link ResultSetFunction}.
     *
     * @param query The sql query
     * @param function The result function to map results.
     * @param <T> The returned type.
     * @return The optional result of query
     * @see #queryAsync(String, ResultSetFunction) to execute in asynchronous thread.
     */
    public <T> Optional<T> query(@Language ( "MySQL" ) @NotNull String query, @NotNull ResultSetFunction<T> function) {
        return query(query, StatementConsumer.EMPTY_STATEMENT, function);
    }

    /**
     * Execute a database statement and retrieve the associated entity in adapters.
     *
     * @param query The sql query.
     * @param clazz The class associated with an adapter.
     * @param <T> The type.
     * @return The optional entity.
     * @see #queryAsync(String, Class) to execute in asynchronous thread
     */
    public <T> Optional<T> query(@Language ( "MySQL" ) @NotNull String query, @NotNull Class<T> clazz) {
        return query(query, StatementConsumer.EMPTY_STATEMENT, resultSet -> resultSet.next() ? getAdapter(clazz).adaptResult(resultSet) : null);
    }

    /**
     * Execute a database statement prepared with @{@link StatementConsumer} and retrieve the associated entity in adapters.
     *
     * @param query The sql query.
     * @param consumer The statement consumer to prepare statement.
     * @param clazz  The class associated with an adapter.
     * @param <T> The type.
     * @return The optional entity.
     * @see #queryAsync(String, StatementConsumer, Class) to execute in asynchronous thread
     */
    public <T> Optional<T> query(@Language ( "MySQL" ) @NotNull String query, @NotNull StatementConsumer consumer, @NotNull Class<T> clazz) {
        return query(query, consumer, resultSet -> resultSet.next() ? getAdapter(clazz).adaptResult(resultSet) : null);
    }

    /**
     * Execute a database statement and retrieve the @{@link ResultSet}.
     *
     * @param query The sql statement.
     * @return The optional result set.
     * @see #queryAsync(String) to execute in asynchronous thread.
     */
    public Optional<ResultSet> query(@Language ( "MySQL" ) @NotNull String query) {
        return query(query, StatementConsumer.EMPTY_STATEMENT);
    }

    /**
     * Execute a database statement prepared with @{@link StatementConsumer} and retrieve the @{@link ResultSet}.
     *
     * @param query The sql statement.
     * @param consumer The statement consumer
     * @return the optional result set.
     * @see #queryAsync(String, StatementConsumer) to execute in asynchronous thread
     */
    public Optional<ResultSet> query(@Language ( "MySQL" ) @NotNull String query, @NotNull StatementConsumer consumer) {
        Objects.requireNonNull(query, "SQL query cannot be null");
        Objects.requireNonNull(consumer, "Statement consumer cannot be null");

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
     * Execute asynchronous database statement prepared with @{@link StatementConsumer} and map their results with @{@link ResultSetFunction}.
     *
     * @param query The sql statement.
     * @param consumer The statement consumer to prepare statement.
     * @param function The result function to map results.
     * @param <T> The returned type.
     * @return The completable future of optional result.
     * @see #query(String, StatementConsumer, ResultSetFunction) to execute in synchronously.
     */
    @NotNull
    @Contract ( "_, _, _ -> new" )
    public <T> CompletableFuture<Optional<T>> queryAsync(@Language ( "MySQL" ) @NotNull String query,
                                                         @NotNull StatementConsumer consumer,
                                                         @NotNull ResultSetFunction<T> function) {
        return supplyAsync(() -> query(query, consumer, function));
    }

    /**
     * Execute asynchronous database statement and map their results with @{@link ResultSetFunction}.
     *
     * @param query The sql query.
     * @param function The result function to map results.
     * @param <T> The returned type.
     * @return The completable future of optional result.
     * @see #query(String, ResultSetFunction) to execute in synchronously
     */
    @NotNull
    @Contract ( "_, _ -> new" )
    public <T> CompletableFuture<Optional<T>> queryAsync(@Language("MySQL") @NotNull String query, @NotNull ResultSetFunction<T> function) {
        return supplyAsync(() -> query(query, function));
    }

    /**
     * Execute asynchronous database statement prepared with @{@link StatementConsumer} and retrieve the associated entity in adapters.
     *
     * @param query The sql query.
     * @param consumer The statement consumer to prepare statement.
     * @param clazz The class associated with an adapter.
     * @param <T>  The type.
     * @return The completable future of optional entity.
     * @see #query(String, StatementConsumer, Class) to execute in synchronously
     */
    @NotNull
    @Contract ( "_, _, _ -> new" )
    public <T> CompletableFuture<Optional<T>> queryAsync(@Language("MySQL") @NotNull String query,
                                                         @NotNull StatementConsumer consumer,
                                                         @NotNull Class<T> clazz) {
        return supplyAsync(() -> query(query, consumer, clazz));
    }

    /**
     * Execute asynchronous database statement and retrieve the associated entity in adapters.
     *
     * @param query The sql query.
     * @param clazz The class associated with an adapter.
     * @param <T> The type.
     * @return The completable future of optional entity.
     * @see #query(String, Class) to execute in synchronously.
     */
    @NotNull
    @Contract ( "_, _ -> new" )
    public <T> CompletableFuture<Optional<T>> queryAsync(@Language ( "MySQL" ) @NotNull String query, @NotNull Class<T> clazz) {
        return supplyAsync(() -> query(query, clazz));
    }

    /**
     * Execute asynchronous database statement and retrieve the @{@link ResultSet}.
     *
     * @param query The sql query.
     * @return The completable future of optional result set.
     * @see #query(String, Class) to execute in synchronously.
     */
    @NotNull
    @Contract ( "_ -> new" )
    public CompletableFuture<Optional<ResultSet>> queryAsync(@Language ( "MySQL" ) @NotNull String query) {
        return supplyAsync(() -> query(query));
    }

    /**
     * Execute asynchronous database statement prepared with @{@link StatementConsumer} and retrieve the @{@link ResultSet}.
     *
     * @param query The sql query.
     * @param consumer The statement consumer to prepare statement.
     * @return The completable future of optional result set.
     * @see #query(String, StatementConsumer) to execute in synchronously.
     */
    @NotNull
    @Contract ( "_, _ -> new" )
    public CompletableFuture<Optional<ResultSet>> queryAsync(@Language("MySQL") @NotNull String query, @NotNull StatementConsumer consumer) {
        return supplyAsync(() -> query(query, consumer));
    }

    /**
     * Execute asynchronous database statement prepared with @{@link StatementConsumer} and retrieve the associated entity in adapters.
     *
     * @param query THe sql query.
     * @param consumer The statement consumer to prepare statement.
     * @param clazz The class associated with an adapter.
     * @param <T>  The type.
     * @return The completable future of set of entities.
     * @see #queryMany(String, StatementConsumer, Class) to execute in synchronously.
     */
    @NotNull
    @Contract ( "_, _, _ -> new" )
    public <T> CompletableFuture<Set<T>> queryManyAsync(@Language ( "MySQL" ) @NotNull String query,
                                                        @NotNull StatementConsumer consumer,
                                                        @NotNull Class<T> clazz) {
        return supplyAsync(() -> queryMany(query, consumer, clazz));
    }

    /**
     * Execute database statement prepared with @{@link StatementConsumer} and retrieve the associated entity in adapters.
     *
     * @param query The sql query.
     * @param consumer The statement consumer to prepare statement.
     * @param clazz The class associated with an adapter.
     * @param <T> The entity type to return.
     * @return The set of entities.
     * @see #queryManyAsync(String, StatementConsumer, Class) to execute in asynchronous.
     */
    @NotNull
    public <T> Set<T> queryMany(@Language ( "MySQL" ) @NotNull String query, @NotNull StatementConsumer consumer, @NotNull Class<T> clazz) {
        Objects.requireNonNull(query, "SQL query cannot be null");
        Objects.requireNonNull(consumer, "Statement consumer cannot be null");
        Objects.requireNonNull(clazz, "Class cannot be null");

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
     * Execute asynchronous database statement and retrieve the associated entity in adapters.
     *
     * @param query The sql query.
     * @param clazz The class to search the adapter.
     * @param <T> The entity type to return.
     * @return The future set of entities.
     * @see #queryMany(String, Class) to execute in synchronous.
     */
    @NotNull
    @Contract ( "_, _ -> new" )
    public <T> CompletableFuture<Set<T>> queryManyAsync(@Language ( "MySQL" ) @NotNull String query, @NotNull Class<T> clazz) {
        return supplyAsync(() -> queryMany(query, clazz));
    }

    /**
     * Execute database statement and retrieve the associated entity in adapters.
     *
     * @param query The sql query.
     * @param clazz The class to search the adapter.
     * @param <T> The entity type to return.
     * @return The future set of entities.
     * @see #queryMany(String, Class) to execute in asynchronous.
     */
    @NotNull
    public <T> Set<T> queryMany(@Language("MySQL") @NotNull String query, @NotNull Class<T> clazz) {
        return queryMany(query, StatementConsumer.EMPTY_STATEMENT, clazz);
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
        executeBatch(builder, Statement.NO_GENERATED_KEYS, StatementConsumer.EMPTY_STATEMENT);
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
     * @param callable the Runnable to be executed within the transaction
     * @param <T> the type of the result.
     * @return a CompletableFuture that will be completed when the transaction and the execution of the runnable are finished
     */
    @NotNull
    public <T> CompletableFuture<T> withTransaction(@NotNull Callable<T> callable) {
        Objects.requireNonNull(callable, "Callable cannot be null");
        return CompletableFuture.supplyAsync(() -> {
            final AtomicReference<T> result = new AtomicReference<T>();

            sqlConnector.execute(connection -> {
                connection.setAutoCommit(false);

                final SQLTransactionHolder transactionHolder = new SQLTransactionHolder(connection, new CurrentThreadExecutor());
                ThreadLocalTransaction.set(transactionHolder);

                try {
                    result.set(callable.call());
                    connection.commit();
                } catch (Throwable t) {
                    connection.rollback();
                    throw new ExecutorException(t);
                } finally {
                    ThreadLocalTransaction.remove();
                }
            });

            return result.get();
        }, executor);
    }

    /**
     * Asynchronous execution that reuse the same executor and connection.
     *
     * @param runnable The runnable to be executed.
     * @return The completable future of the execution.
     */
    @NotNull
    @Contract("_ -> new")
    private CompletableFuture<Void> runAsync(@NotNull Runnable runnable) {
        return CompletableFuture.runAsync(runnable, getCurrentExecutor());
    }

    /**
     * Asynchronous execution that reuse the same executor and connection.
     *
     * @param supplier The supplier to be executed.
     * @param <T> The returned type.
     * @return The completable future of the execution.
     */
    @NotNull
    @Contract("_ -> new")
    private <T> CompletableFuture<T> supplyAsync(@NotNull Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, getCurrentExecutor());
    }

}
