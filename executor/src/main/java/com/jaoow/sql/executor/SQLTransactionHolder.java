package com.jaoow.sql.executor;

import lombok.Data;

import java.sql.Connection;
import java.util.concurrent.Executor;

@Data
public class SQLTransactionHolder {
    private final Connection connection;
    private final Executor executor;
}
