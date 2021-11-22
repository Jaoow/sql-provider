package com.jaoow.sql.executor.batch;

import com.jaoow.sql.executor.SQLExecutor;
import com.jaoow.sql.executor.statement.SimpleStatement;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class BatchBuilder {

    @NotNull private final String statement;
    @NotNull private final SQLExecutor executor;
    @NotNull private final List<Consumer<SimpleStatement>> handlers = new LinkedList<>();

    public BatchBuilder(@Language("MySQL") @NotNull String statement, @NotNull SQLExecutor executor) {
        this.statement = statement;
        this.executor = executor;
    }

    /**
     * Gets the statement to be executed when this batch is finished.
     *
     * @return the statement to be executed
     */
    @NotNull
    public String getStatement() {
        return this.statement;
    }

    /**
     * Gets a {@link List} of handlers for this statement.
     *
     * @return the handlers for this statement
     */
    @NotNull
    public List<Consumer<SimpleStatement>> getHandlers() {
        return this.handlers;
    }

    /**
     * Resets this BatchBuilder, making it possible to re-use
     * for multiple situations.
     *
     * @return this builder
     */
    public BatchBuilder reset() {
        this.handlers.clear();
        return this;
    }

    /**
     * Adds a handler to be executed when this batch is finished.
     *
     * @param handler the statement handler
     * @return this builder
     */
    public BatchBuilder batch(@NotNull Consumer<SimpleStatement> handler) {
        this.handlers.add(handler);
        return this;
    }

    /**
     * Executes the statement for this batch, with the handlers used to prepare it.
     */
    public void execute() {
        this.executor.executeBatch(this);
    }

    /**
     * Executes the statement for this batch, with the handlers used to prepare it.
     *
     * Will return a {@link CompletableFuture} to do this.
     *
     * @return a promise to execute this batch asynchronously
     */
    @NotNull
    public CompletableFuture<Void> executeAsync() {
        return this.executor.executeBatchAsync(this);
    }
}
