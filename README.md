# SQL Provider
SQL provider is a lightweight library to handle SQL operations.

If you would like to use this library as a dependency in your project, you can use maven:
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
    <version>1.0</version>
</dependency>
```

## Basic Setup
```java

    // Setup SQLite Connection
    private SQLExecutor setupSQLite() throws SQLException {
        
        SQLiteDatabaseType databaseType = SQLiteDatabaseType.builder()
                .file(new File("databases/database.db"))
                .build();

        return new SQLExecutor(databaseType.connect());
    }

    // Setup MYSQL Connection
    private SQLExecutor setupMySQL() throws SQLException {

        MySQLDatabaseType databaseType = MySQLDatabaseType.builder()
                .address("localhost:3306")
                .database("database")
                .username("root")
                .password("")
                .build();

        return new SQLExecutor(databaseType.connect());
    }
```

Contributing
------
SQL Provider is an open source project, and gladly accepts community contributions.

-----
###### This project was based on [sql-provider by HenryFabio](https://github.com/HenryFabio/sql-provider) with some modifications
