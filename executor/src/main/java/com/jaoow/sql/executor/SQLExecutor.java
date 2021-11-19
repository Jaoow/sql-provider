package com.jaoow.sql.executor;

import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.executor.adapter.SQLResultAdapter;
import com.jaoow.sql.executor.result.SimpleResultSet;
import com.jaoow.sql.executor.statement.SimpleStatement;
import lombok.AllArgsConstructor;
import lombok.Builder;
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

@RequiredArgsConstructor @AllArgsConstructor
public final class SQLExecutor {

    @NotNull private final SQLConnector sqlConnector;
    @NotNull private final Map<Class<?>, SQLResultAdapter<?>> adapterMap;

    @NotNull
    private Executor executor = ForkJoinPool.commonPool();

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
     * @param clazz the type of class of adapter
     * @param <T> the returned type
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
     * @param sql the sql statement
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
     * @param sql the sql statement
     * @param consumer the @{@link SimpleStatement} to prepare statement
     * @see #executeAsync(String, Consumer) to execute statement in asynchronous thread
     */
    public void execute(@Language("MySQL") @NotNull String sql, @NotNull Consumer<SimpleStatement> consumer) {
        sqlConnector.execute(connection -> {
            try(SimpleStatement statement = SimpleStatement.of(connection.prepareStatement(sql))) {
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
        executeAsync(sql, simpleStatement -> {});
    }

    /**
     * Execute a database query in asynchronous thread.
     *
     * @param query the sql query
     * @param consumer the @{@link SimpleStatement} to prepare query
     * @param function the function to map @{@link SimpleResultSet}
     * @param <T> the returned type
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
     * @param query the sql query
     * @param consumer the @{@link SimpleStatement} to prepare query
     * @param function the function to map @{@link SimpleResultSet}
     * @param <T> the returned type
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
     * @param query the sql query
     * @param function the function to map @{@link SimpleResultSet}
     * @param <T> the returned type
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
     * @param query the sql query
     * @param function the function to map @{@link SimpleResultSet}
     * @param <T> the returned type
     * @return the optional result of query
     * @see #queryAsync(String, Function) to execute in asynchronous thread.
     */
    public <T> Optional<T> query(@Language("MySQL") @NotNull String query, @NotNull Function<SimpleResultSet, T> function) {
        return query(query, statement -> {}, function);
    }

    /**
     * Execute a database query in asynchronous thread.
     *
     * @param query the sql query
     * @param consumer the @{@link SimpleStatement} to prepare query
     * @param adapter the adapter to map @{@link SimpleResultSet}
     * @param <T> the returned type
     * @return the completable future of optional query result
     * @see #query(String, Consumer, SQLResultAdapter) to execute in synchronously
     */
    public <T> CompletableFuture<Optional<T>> queryAsync(@Language("MySQL") @NotNull String query,
                                                         @NotNull Consumer<SimpleStatement> consumer,
                                                         @NotNull SQLResultAdapter<T> adapter
    ) {
        return CompletableFuture.supplyAsync(() -> query(query, consumer, adapter), executor);
    }

    /**
     * Execute a database query.
     *
     * @param query the sql query
     * @param consumer consumer the @{@link SimpleStatement} to prepare queue
     * @param mapper the @{@link SQLResultAdapter} to map result
     * @param <T> the returned type
     * @return the optional result of query
     * @see #queryAsync(String, Consumer, SQLResultAdapter) to execute in asynchronous thread
     */
    public <T> Optional<T> query(@Language("MySQL") @NotNull String query,
                                 @NotNull Consumer<SimpleStatement> consumer,
                                 @NotNull SQLResultAdapter<T> mapper
    ) {
        return query(query, consumer, mapper::adaptResult);
    }

    /**
     * Execute a database query.
     *
     * @param query the sql query
     * @param consumer the @{@link SimpleResultSet} to prepare query
     * @param clazz the class to search @{@link SQLResultAdapter}
     * @param <T> the returned type
     * @return the completable future of optional query result
     * @see #resultQuery(String, Consumer, Class) to execute in synchronously
     * @throws NullPointerException if {@link SQLResultAdapter} is null
     */
    public <T> CompletableFuture<Optional<T>> resultQueryAsync(@Language("MySQL") @NotNull String query,
                                                               @NotNull Consumer<SimpleStatement> consumer,
                                                               @NotNull Class<T> clazz
    ) {
        return CompletableFuture.supplyAsync(() -> resultQuery(query, consumer, clazz), executor);
    }

    /**
     * Select a single entity from a given SQL query
     *
     * @param <T>           The entity type to return.
     * @param query         The query to select the entity.
     * @param consumer      The statement consumer
     * @param clazz         The class to search adapter
     * @return The entity founded, or null
     * @see #resultQueryAsync(String, Consumer, Class) to execute in asynchronous thread
     * @throws NullPointerException if {@link SQLResultAdapter} is null
     */
    public <T> Optional<T> resultQuery(@Language("MySQL") @NotNull String query,
                                       @NotNull Consumer<SimpleStatement> consumer,
                                       @NotNull Class<T> clazz
    ) {
        SQLResultAdapter<T> adapter = getAdapter(clazz);
        Objects.requireNonNull(adapter, "the adapter for class " + clazz.getSimpleName() + " was not found.");

        return query(query, consumer, resultSet -> resultSet.next() ? adapter.adaptResult(resultSet) : null);
    }

    /**
     * Execute a database query.
     *
     * @param query the sql query
     * @param clazz the class to search adapter
     * @param <T> the returned type
     * @return the completable future of optional query result
     * @see #resultQuery(String, Consumer, Class) to execute in synchronously
     * @throws NullPointerException if {@link SQLResultAdapter} is null
     */
    public <T> CompletableFuture<Optional<T>> resultQueryAsync(@Language("MySQL") @NotNull String query,
                                                               @NotNull Class<T> clazz
    ) {
        return CompletableFuture.supplyAsync(() -> resultQuery(query, statement -> {}, clazz), executor);
    }

    /**
     * Select a single entity from a given SQL query
     *
     * @param <T> the entity type to return.
     * @param query the query to select the entity.
     * @param clazz the class to search @{@link SQLResultAdapter}
     * @return the entity founded, or null
     * @see #resultQueryAsync(String, Class)  to execute in asynchronous thread
     * @throws NullPointerException if {@link SQLResultAdapter} is null
     */
    public <T> Optional<T> resultQuery(@Language("MySQL") @NotNull String query, @NotNull Class<T> clazz) {
        SQLResultAdapter<T> adapter = getAdapter(clazz);
        Objects.requireNonNull(adapter, "the adapter for class " + clazz.getSimpleName() + " was not found.");

        return query(query, statement -> {}, resultSet -> resultSet.next() ? adapter.adaptResult(resultSet) : null);
    }

    /**
     * Execute a database query
     *
     * @param query the sql query
     * @param consumer the @{@link SimpleStatement} to prepare query
     * @param clazz the class to search @{@link SQLResultAdapter}
     * @param <T> the returned type
     * @return the completable future of @{@link Set} of result
     * @throws NullPointerException if {@link SQLResultAdapter} is null
     */
    public <T> CompletableFuture<Set<T>> resultSetQueryAsync(@Language("MySQL") @NotNull String query,
                                                             @NotNull Consumer<SimpleStatement> consumer,
                                                             @NotNull Class<T> clazz) {

        return CompletableFuture.supplyAsync(() -> resultSetQuery(query, consumer, clazz), executor);
    }

    /**
     * Select entities from a given SQL query
     *
     * @param <T> the entity type to return
     * @param query the query to select entities
     * @param consumer the statement consumer
     * @param clazz the class to search @{@link SQLResultAdapter}
     * @return The entities found
     * @see #resultSetQueryAsync(String, Consumer, Class) to execute in asynchronous thread
     * @throws NullPointerException if {@link SQLResultAdapter} is null
     */
    public <T> Set<T> resultSetQuery(@Language("MySQL") @NotNull String query,
                                     @NotNull Consumer<SimpleStatement> consumer,
                                     @NotNull Class<T> clazz
    ) {
        SQLResultAdapter<T> adapter = getAdapter(clazz);
        Objects.requireNonNull(adapter, "the adapter for class " + clazz.getSimpleName() + " was not found.");

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
     * @param <T> the returned type
     * @return the completable future of @{@link Set} of result
     * @see #resultSetQuery(String, Class) to execute in synchronously
     * @throws NullPointerException if {@link SQLResultAdapter} is null
     */
    public <T> CompletableFuture<Set<T>> resultSetQueryAsync(@Language("MySQL") @NotNull String query, @NotNull Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> resultSetQuery(query, clazz), executor);
    }

    /**
     * Select entities from a given SQL query
     *
     * @param <T> the entity type to return
     * @param query the query to select entities
     * @param clazz the class to search @{@link SQLResultAdapter}
     * @return The entities found
     * @see #resultSetQueryAsync(String, Class) to execute in asynchronous thread
     * @throws NullPointerException if {@link SQLResultAdapter} is null
     */
    public <T> Set<T> resultSetQuery(@Language("MySQL") @NotNull String query, @NotNull Class<T> clazz) {
        return resultSetQuery(query, statement -> {}, clazz);
    }

}
