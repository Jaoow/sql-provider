package com.jaoow.sql.executor;

import java.util.concurrent.Executor;

/**
 * This class is a implementation of Executor
 * It is used to run a given command in the current thread (synchronous).
 */
public class CurrentThreadExecutor implements Executor {
    @Override
    public void execute(Runnable command) {
        command.run();
    }
}