# SQL Provider [![Codacy Badge](https://app.codacy.com/project/badge/Grade/698f97b0b74b49058022cc55eeacbca9)](https://www.codacy.com/gh/Jaoow/sql-provider/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Jaoow/sql-provider&amp;utm_campaign=Badge_Grade) [![GitHub stars](https://img.shields.io/github/stars/Jaoow/sql-provider.svg)](https://github.com/Jaoow/sql-provider/stargazers) [![Open Source Love](https://badges.frapsoft.com/os/v1/open-source.svg?v=103)](https://github.com/ellerbrock/open-source-badges/) [![](https://jitpack.io/v/Jaoow/sql-provider.svg)](https://jitpack.io/#Jaoow/sql-provider)
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
