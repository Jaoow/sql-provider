plugins {
    id("java")
    id("maven")
    id("maven-publish")
}

group = "com.jaoow"
version = "1.0.0"

allprojects {
    plugins.apply("java")
    plugins.apply("maven")
    plugins.apply("maven-publish")

    repositories {
        mavenCentral()
        jcenter()
    }

    dependencies {
        val lombokVersion = "1.18.22"
        val annotationsVersion = "21.0.1"

        val hikariVersion = "4.0.3"
        val slf4jVersion = "1.7.32"
        val sqliteVersion = "3.36.0.2"
        val mysqlVersion = "8.0.15"
        val mariaDBVersion = "2.4.2";

        annotationProcessor("org.projectlombok:lombok:$lombokVersion")
        annotationProcessor("org.jetbrains:annotations:$annotationsVersion")

        compileOnly("org.projectlombok:lombok:$lombokVersion")
        compileOnly("org.jetbrains:annotations:$annotationsVersion")

        implementation("com.zaxxer:HikariCP:$hikariVersion")
        implementation("org.slf4j:slf4j-api:$slf4jVersion")
        implementation("org.slf4j:slf4j-simple:$slf4jVersion")

        compileOnly("org.xerial:sqlite-jdbc:$sqliteVersion")

        testImplementation("mysql:mysql-connector-java:$mysqlVersion")
        testImplementation("org.mariadb.jdbc:mariadb-java-client:$mariaDBVersion")
    }
}

tasks.withType<Javadoc> {
    setDependsOn(setOf("jar"))

    source(subprojects.flatMap { it.sourceSets.main.get().allJava })
    setDestinationDir(file("$buildDir/docs/javadoc"));

    options.header("").addStringOption("https://docs.oracle.com/javase/8/docs/api/")
}