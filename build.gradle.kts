plugins {
    id("java")
    id("maven-publish")
}

group = "com.jaoow"
version = "1.0.0"

allprojects {
    plugins.apply("java")
    plugins.apply("maven-publish")

    repositories {
        mavenCentral()
    }

    dependencies {
        val lombokVersion = "1.18.22"
        val annotationsVersion = "21.0.1"

        val hikariVersion = "4.0.3"
        val slf4jVersion = "1.7.32"
        val sqliteVersion = "3.45.1.0"
        val mysqlVersion = "8.0.33"
        val mariaDBVersion = "3.3.2";

        // Lombok
        annotationProcessor("org.projectlombok:lombok:$lombokVersion")
        compileOnly("org.projectlombok:lombok:$lombokVersion")

        // Jetbrains Annotations
        compileOnly("org.jetbrains:annotations-java5:$annotationsVersion")

        // HikariCP
        implementation("com.zaxxer:HikariCP:$hikariVersion")
        implementation("org.slf4j:slf4j-api:$slf4jVersion")
        implementation("org.slf4j:slf4j-simple:$slf4jVersion")

        // Databases
        implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
        implementation("mysql:mysql-connector-java:$mysqlVersion")
        implementation("org.mariadb.jdbc:mariadb-java-client:$mariaDBVersion")
    }
}

tasks.withType<Javadoc> {
    setDependsOn(setOf("clean"))

    source(subprojects.flatMap { it.sourceSets.main.get().allJava })
    setDestinationDir(file("${layout.buildDirectory.get()}/docs/javadoc"))

    options.header("").apply {
        links("https://docs.oracle.com/javase/8/docs/api/")
        linksOffline("https://javadoc.io/doc/org.jetbrains/annotations/latest/", "${projectDir}/javadoc")
    }
}