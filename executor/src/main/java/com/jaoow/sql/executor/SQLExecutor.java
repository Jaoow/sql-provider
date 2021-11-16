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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
     * execute a database statement in asynchronous thread
     *
     * @param sql the sql statement
     * @param consumer the @{@link SimpleStatement} to prepare statement
     * @return the completable future of execution
     * @see #execute(String, Consumer) to execute statement in synchronously
     */
    public CompletableFuture<Void> executeAsync(@Language("MySQL") @NotNull String sql,
                                                @NotNull Consumer<SimpleStatement> consumer) {

        return CompletableFuture.runAsync(() -> execute(sql, consumer), executor);
    }

    /**
     * execute a database statement.
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
     * execute a database statement in asynchronous thread
     *
     * @param sql the sql statement
     * @see #execute(String) to execute statment in synchronously.
     */
    public void executeAsync(@Language("MySQL") @NotNull String sql) {
        CompletableFuture.runAsync(() -> execute(sql), executor);
    }

    /**
     * execute a database statement.
     *
     * @param sql the sql statement
     * @see #executeAsync(String) to execute statement in asynchronous thread
     */
    public void execute(@Language("MySQL") @NotNull String sql) {
        executeAsync(sql, simpleStatement -> {});
    }

    /**
     * Select entities from a given SQL query
     *
     * @param <T>      The entity type to return
     * @param query    The query to select entities
     * @param consumer The statement consumer
     * @param mapper   The mapper used to build entities
     * @return The entities found
     */
    public <T> T resultQuery(String query, Consumer<SimpleStatement> consumer, SQLResultAdapter<T> mapper) {
        AtomicReference<T> value = new AtomicReference<>();
        sqlConnector.execute(connection -> {
            try (SimpleStatement statement = SimpleStatement.of(connection.prepareStatement(query))) {
                consumer.accept(statement);

                try (SimpleResultSet resultSet = statement.executeQuery()) {
                    value.set(mapper.adaptResult(resultSet));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return value.get();
    }

    /**
     * Select a single entity from a given SQL query
     *
     * @param <T>           The entity type to return.
     * @param query         The query to select the entity.
     * @param consumer      The statement consumer
     * @param resultAdapter The adapter used to build the entity.
     * @return The entity founded, or null
     */
    public <T> T resultOneQuery(String query,
                                Consumer<SimpleStatement> consumer,
                                Class<? extends SQLResultAdapter<T>> resultAdapter
    ) {
        return resultQuery(query, consumer, resultSet -> {

            if (resultSet.next()) {
                SQLResultAdapter<T> adapter = adapterProvider.getAdapter(resultAdapter);
                return adapter.adaptResult(resultSet);
            }

            return null;
        });
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
    public <T> Set<T> resultManyQuery(String query,
                                      Consumer<SimpleStatement> consumer,
                                      Class<? extends SQLResultAdapter<T>> resultAdapter
    ) {
        return this.resultQuery(query, consumer, resultSet -> {
            SQLResultAdapter<T> adapter = adapterProvider.getAdapter(resultAdapter);
            Set<T> elements = new LinkedHashSet<>();
            while (resultSet.next()) {
                elements.add(adapter.adaptResult(resultSet));
            }

            return elements;
        });
    }
}
