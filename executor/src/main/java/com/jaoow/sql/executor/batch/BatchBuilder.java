/*
 * This file is part of helper, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.jaoow.sql.executor.batch;

import com.jaoow.sql.executor.SQLExecutor;
import com.jaoow.sql.executor.function.StatementConsumer;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BatchBuilder {

    @NotNull private final String statement;
    @NotNull private final SQLExecutor executor;
    @NotNull private final List<StatementConsumer> handlers = new LinkedList<>();

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
    public List<StatementConsumer> getHandlers() {
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
    public BatchBuilder batch(@NotNull StatementConsumer handler) {
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
