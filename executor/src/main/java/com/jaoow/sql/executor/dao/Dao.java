package com.jaoow.sql.executor.dao;

import com.jaoow.sql.executor.SQLExecutor;
import com.jaoow.sql.executor.adapter.SQLResultAdapter;
import com.jaoow.sql.executor.batch.BatchBuilder;
import com.jaoow.sql.executor.exceptions.SQLAdapterNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Class to handle database objects more easily
 */
@RequiredArgsConstructor
public class Dao<K, V> {

    private final String table;
    private final Class<V> clazz;
    private final Function<K, String> keyAdapter;

    private final BatchBuilder batch;
    private final SQLExecutor executor;

    /**
     * Create an instance of @{@link Dao}
     */
    public Dao(@NotNull String table, @NotNull Class<V> clazz, @NotNull Function<K, String> keyAdapter, @NotNull SQLExecutor executor) {
        this.table = table;
        this.clazz = clazz;
        this.keyAdapter = keyAdapter;
        this.executor = executor;
        this.batch = executor.batch("REPLACE INTO " + table + " VALUES(?, ?)");
    }


    /**
     * Initialises the table.
     */
    public void createTable() {
        executor.execute("CREATE TABLE IF NOT EXISTS " + table + "(id VARCHAR(128) NOT NULL PRIMARY KEY UNIQUE, data TEXT);");
    }

    /**
     * Get all {@code data objects} from database.
     *
     * @return a set with all objects
     */
    @NotNull
    public Set<V> selectAll() {
        return executor.queryMany("SELECT * FROM " + table, clazz);
    }

    /**
     * Gets the {@code data object} from database.
     *
     * @param key the object key
     * @return an optional with the object
     */
    public Optional<V> selectOne(K key) {
        return executor.query("SELECT * FROM " + table + " WHERE id = ?",
                statement -> statement.set(1, key.toString()), clazz
        );
    }

    /**
     * Delete the specified {@code key} from database.
     *
     * @param key the object key
     * @return a future encapsulating the operation
     */
    public CompletableFuture<Void> deleteOne(K key) {
        return executor.executeAsync(
                "DELETE FROM " + table + " WHERE id = ?",
                statement -> statement.set(1, key.toString())
        );
    }

    /**
     * Save a specified {@code data object} in database.
     *
     * @param value the object to save
     * @return a future encapsulating the operation
     */
    public CompletableFuture<Void> saveOne(V value) {
        return saveAll(Collections.singleton(value));
    }

    /**
     * Save a set of {@code data s} in database.
     *
     * @param values the set of object to save
     * @return a future encapsulating the operation
     */
    public CompletableFuture<Void> saveAll(Set<V> values) {

        SQLResultAdapter<V> adapter = executor.getAdapter(clazz);

        if (adapter == null) {
            throw new SQLAdapterNotFoundException(clazz);
        }

        for (V value : values) {
            batch.batch(adapter.adaptStatement(value));
        }

        return batch.executeAsync().thenRun(batch::reset);
    }
}
