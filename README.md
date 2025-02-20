# SQL Provider

[![Open Source Love](https://badges.frapsoft.com/os/v1/open-source.svg?v=100)](https://github.com/ellerbrock/open-source-badges/)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/698f97b0b74b49058022cc55eeacbca9)](https://app.codacy.com/gh/Jaoow/sql-provider/dashboard)
[![Javadoc](https://img.shields.io/badge/JavaDoc-Online-green)](https://jaoow.github.io/sql-provider/)
[![GitHub Repo stars](https://img.shields.io/github/stars/Jaoow/sql-provider?style=plastic&color=lime_green)](https://github.com/Jaoow/sql-provider/stargazers)

SQL provider is a lightweight library to handle SQL operations.

## Summary

- [Installation](#installation)
  - [Maven](#maven)
  - [Gradle](#gradle)
- [Initializing connector](#initializing-connector)
  - [SQLite](#sqlite)
  - [MySQL](#mysql)
- [Performing Operations](#performing-operations)
  - [Basic Operations](#basic-operations)
  - [Batch Operations](#batch-operations)
  - [Transactions](#transactions)
  - [Adapters](#adapters)

## Installation

[![Jitpack](https://jitpack.io/v/Jaoow/sql-provider.svg)](https://jitpack.io/#Jaoow/sql-provider)

If you want to add this library to your project, you can use either
[Maven](#maven) or [Gradle](#gradle). However, remember to change
**{version}** to the current version of the project informed in the embed above.

### Maven

```xml
<repository>
    <id>jitpack</id>
    <url>https://jitpack.io</url>
</repository>
```

```xml
<dependency>
    <groupId>com.github.Jaoow</groupId>
    <artifactId>sql-provider</artifactId>
    <version>{version}</version>
</dependency>
```

### Gradle

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Jaoow:sql-provider:{version}'
}
```

## Initializing connector

Firstly, you need to configure the database settings to start using it.
Currently, three connection options are available: [SQLite](#sqlite),
[MySQL and MariaDB](#mysql). For MariaDB the same configuration is used as
for MySQL, however the class to be used is
[MariaDatabaseType](https://jaoow.github.io/sql-provider/javadoc/com/jaoow/sql/connector/type/impl/MySQLDatabaseType.html).

### SQLite

For SQLite, you can simply follow this configuration and enter the database file.

```java
SQLConnector connector = new SQLiteDatabaseType(new File("database.db")).connect();
```

### MySQL

```java
MySQLDatabaseType type = MySQLDatabaseType.builder()
    .address("ip:port")
    .database("database")
    .username("user")
    .password("password")
    .build();

SQLConnector connector = type.connect();
```

> :memo: **Note:**  When using the `connect()` method, an attempt will be made
to connect to the database.

## Performing operations

To perform operations on the database, [SQLExecutor](https://jaoow.github.io/sql-provider/javadoc/com/jaoow/sql/executor/SQLExecutor.html)
will be used, obtained as follows:

```java
SQLExecutor executor = new SQLExecutor(connector);
```

### Basic Operations

The executor has several methods to perform operations on the database.
For example, to create the table.

```java
executor.execute("CREATE TABLE table (col1 VARCHAR(16), col2 VARCHAR(16))");
```

Examples for inserting values into the table or other similar operations.
Note that you need to define the argument as `StatementConsumer` due to the
possibility of directly obtaining the ResultSet, you will have an example
of this later.

```java
executor.execute("INSERT INTO table (col1, col2) VALUES (?, ?)",
                (StatementConsumer) statement -> {

    statement.setString(1, "value1");
    statement.setString(2, "value2");
});
```

```java
executor.executeAsync("INSERT INTO table (col1, col2) VALUES (?, ?)", 
                (StatementConsumer) statement -> {
    
    statement.setString(1, "value1");
    statement.setString(2, "value2");
}).whenComplete((unused, error) -> {
    // TODO()
});
```

Performing an operation to return selected rows in the database.
Note that in this example the argument was defined as `ResultSetConsumer`
to obtain the ResultSet.

```java
executor.execute("SELECT * FROM table", (ResultSetConsumer) result -> {
    while (result.next()) {
        System.out.println(result.getString("column1"));
    }
});
```

### Batch Operations

Batch operations can be used to improve the efficiency of operations
by processing data together instead of one at a time. Batches can also
reduce latency and network consumption.

```java
BatchBuilder builder = executor.batch("UPDATE users SET age = ? WHERE name = ?");

for (User user : users) {
    builder.batch(statement -> {
        statement.setInt(1, user.getAge());
        statement.setString(2, user.getName());
    });
}

builder.execute();
```

### Transactions

Transactions are used to ensure that a set of operations are executed
on the same connection, without the need to open another one.

```java
executor.withTransaction(transaction -> {
    User user1 = executor.query("SELECT * FROM table WHERE name = 'Test'", User.class);
    User user2 = executor.query("SELECT * FROM table WHERE name = 'Test2'", User.class);
    
    // Some operations
    return null;
    
}).whenComplete((unused, error) -> {
    // TODO()
});
```

This is not a good example of why you can select everyone with just one query,
but it is just an example of why you can do several operations that will use
the same connection.

### Adapters

Adapters are a good option for making selections and transforming the ResultSet
directly into an object. Adapters can be created as follows:

```java
public class UserAdapter implements SQLResultAdapter<User> {

    @Nullable
    @Override
    public User adaptResult(@NotNull ResultSet set) throws SQLException {
        String name = set.getString("name");
        int age = set.getInt("age");
        
        return new User(name, age);
    }
    
}
```

After creation, it is still necessary to register it with the executor
to be able to use it.

```java
executor.registerAdapter(User.class, new UserAdapter());
```

With this you can transform the results directly into objects.

```java
Set<User> users = executor.queryMany("SELECT * FROM table", User.class);
```

```java
Optional<User> user = executor.query("SELECT * FROM table WHERE name = ?",
        statement -> statement.setString(1, "Test"),
        User.class);
```

## Contributing

SQL Provider is an open source project, and gladly accepts community contributions.

-----

> This project was based on
> [sql-provider by HenryFabio](https://github.com/HenryFabio/sql-provider)
> with some modifications
