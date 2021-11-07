package com.jaoow.sql.executor;

import com.jaoow.sql.connector.SQLConnector;
import com.jaoow.sql.executor.adapter.SQLResultAdapter;
import com.jaoow.sql.executor.adapter.SQLResultAdapterProvider;
import com.jaoow.sql.executor.result.SimpleResultSet;
import com.jaoow.sql.executor.statement.SimpleStatement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

@Getter
@RequiredArgsConstructor
public final class SQLExecutor {

    private final SQLConnector sqlConnector;
    private final SQLResultAdapterProvider adapterProvider = SQLResultAdapterProvider.getInstance();

    /**
     * Execute a query for UPDATE, INSERT or DELETE
     *
     * @param query    The query to execute
     * @param consumer The statement consumer
     */
    public void updateQuery(String query, Consumer<SimpleStatement> consumer) {
        sqlConnector.execute(connection -> {
            try (SimpleStatement statement = SimpleStatement.of(connection.prepareStatement(query))) {
                consumer.accept(statement);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Execute a query for UPDATE, INSERT or DELETE
     *
     * @param query The query to execute
     */
    public void updateQuery(String query) {
        updateQuery(query, statement -> {});
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
