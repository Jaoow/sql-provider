package com.jaoow.sql.executor;

import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.executor.adapter.SQLResultAdapter;
import com.jaoow.sql.executor.adapter.SQLResultAdapterProvider;
import com.jaoow.sql.executor.result.SimpleResultSet;
import com.jaoow.sql.executor.statement.SimpleStatement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

@RequiredArgsConstructor
public final class SQLExecutor {

    @NotNull private final SQLConnector sqlConnector;
    @NotNull private Executor executor = ForkJoinPool.commonPool();

    @Getter private final SQLResultAdapterProvider adapterProvider = SQLResultAdapterProvider.getInstance();

    /**
     * Set the executor to perform asynchronous statements
     *
     * @param executor tbe @{@link Executor}
     */
    public void setExecutor(@NotNull Executor executor) {
        this.executor = executor;
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
     * @see #execute(String) to execute statment in synchronously.
     */
    public void executeAsync(@Language("MySQL") @NotNull String sql) {
        CompletableFuture.runAsync(() -> execute(sql), executor);
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
     * @param adapter the adapter to map @{@link SimpleResultSet}
     * @param <T> the returned type
     * @return the completable future of optional query result
     * @see #query(String, Consumer, SQLResultAdapter) to execute in synchronously
     */
    public <T> CompletableFuture<Optional<T>> queryAsync(@Language("MySQL") @NotNull String query,
                                                         @NotNull SQLResultAdapter<T> adapter
    ) {
        return CompletableFuture.supplyAsync(() -> query(query, adapter), executor);
    }

    /**
     * Execute a database query.
     *
     * @param query the sql query
     * @param adapter the adapter to map @{@link SimpleResultSet}
     * @param <T> the returned type
     * @return the optional result of query
     * @see #queryAsync(String, SQLResultAdapter) to execute in asynchronous thread.
     */
    public <T> Optional<T> query(@Language("MySQL") @NotNull String query, @NotNull SQLResultAdapter<T> adapter) {
        return query(query, statement -> {}, adapter);
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
     * @param adapter the @{@link SQLResultAdapter} to map result set
     * @param <T> the returned type
     * @return the completable future of optional query result
     * @see #resultQuery(String, Consumer, Class) to execute in synchronously
     */
    public <T> CompletableFuture<Optional<T>> resultQueryAsync(@Language("MySQL") @NotNull String query,
                                                               @NotNull Consumer<SimpleStatement> consumer,
                                                               @NotNull Class<? extends SQLResultAdapter<T>> adapter
    ) {
        return CompletableFuture.supplyAsync(() -> resultQuery(query, consumer, adapter), executor);
    }

    /**
     * Select a single entity from a given SQL query
     *
     * @param <T>           The entity type to return.
     * @param query         The query to select the entity.
     * @param consumer      The statement consumer
     * @param resultAdapter The adapter used to build the entity.
     * @return The entity founded, or null
     * @see #resultQueryAsync(String, Consumer, Class) to execute in asynchronous thread
     */
    public <T> Optional<T> resultQuery(@Language("MySQL") @NotNull String query,
                                       @NotNull Consumer<SimpleStatement> consumer,
                                       @NotNull Class<? extends SQLResultAdapter<T>> resultAdapter
    ) {
        return query(query, consumer, resultSet -> {

            if (resultSet.next()) {
                SQLResultAdapter<T> adapter = adapterProvider.getAdapter(resultAdapter);
                return adapter.adaptResult(resultSet);
            }

            return null;
        });
    }

    /**
     * Execute a database query.
     *
     * @param query the sql query
     * @param adapter the @{@link SQLResultAdapter} to map result set
     * @param <T> the returned type
     * @return the completable future of optional query result
     * @see #resultQuery(String, Consumer, Class) to execute in synchronously
     */
    public <T> CompletableFuture<Optional<T>> resultQueryAsync(@Language("MySQL") @NotNull String query,
                                                               @NotNull Class<? extends SQLResultAdapter<T>> adapter
    ) {
        return CompletableFuture.supplyAsync(() -> resultQuery(query, statement -> {}, adapter), executor);
    }

    /**
     * Select a single entity from a given SQL query
     *
     * @param <T>           The entity type to return.
     * @param query         The query to select the entity.
     * @param resultAdapter The adapter used to build the entity.
     * @return The entity founded, or null
     * @see #resultQueryAsync(String, Class)  to execute in asynchronous thread
     */
    public <T> Optional<T> resultQuery(@Language("MySQL") @NotNull String query,
                                       @NotNull Class<? extends SQLResultAdapter<T>> resultAdapter
    ) {
        return query(query, statement -> {}, resultSet -> {

            if (resultSet.next()) {
                SQLResultAdapter<T> adapter = adapterProvider.getAdapter(resultAdapter);
                return adapter.adaptResult(resultSet);
            }

            return null;
        });
    }

    /**
     * Execute a database query
     *
     * @param query the sql query
     * @param consumer the @{@link SimpleStatement} to prepare query
     * @param adapter the @{@link SQLResultAdapter} to map result
     * @param <T> the returned type
     * @return the completable future of @{@link Set} of result
     */
    public <T> CompletableFuture<Set<T>> resultSetQueryAsync(@Language("MySQL") @NotNull String query,
                                                             @NotNull Consumer<SimpleStatement> consumer,
                                                             @NotNull Class<? extends SQLResultAdapter<T>> adapter) {

        return CompletableFuture.supplyAsync(() -> resultSetQuery(query, consumer, adapter), executor);
    }

    /**
     * Select entities from a given SQL query
     *
     * @param <T>           The entity type to return
     * @param query         The query to select entities
     * @param consumer      The statement consumer
     * @param resultAdapter The adapter used to build the entities.
     * @return The entities found
     */
    public <T> Set<T> resultSetQuery(@Language("MySQL") @NotNull String query,
                                     @NotNull Consumer<SimpleStatement> consumer,
                                     @NotNull Class<? extends SQLResultAdapter<T>> resultAdapter
    ) {
        return this.query(query, consumer, result -> {
            SQLResultAdapter<T> adapter = adapterProvider.getAdapter(resultAdapter);
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
     * @param adapter the @{@link SQLResultAdapter} to map result
     * @param <T> the returned type
     * @return the completable future of @{@link Set} of result
     */
    public <T> CompletableFuture<Set<T>> resultSetQueryAsync(@Language("MySQL") @NotNull String query,
                                                             @NotNull Class<? extends SQLResultAdapter<T>> adapter) {

        return CompletableFuture.supplyAsync(() -> resultSetQuery(query, adapter), executor);
    }

    /**
     * Select entities from a given SQL query
     *
     * @param <T>           The entity type to return
     * @param query         The query to select entities
     * @param resultAdapter The adapter used to build the entities.
     * @return The entities found
     */
    public <T> Set<T> resultSetQuery(@Language("MySQL") @NotNull String query,
                                     @NotNull Class<? extends SQLResultAdapter<T>> resultAdapter
    ) {
        return resultSetQuery(query, statement -> {}, resultAdapter);
    }

}
