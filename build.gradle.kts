plugins {
    id("java-library")
    id("maven-publish")
}

group = "com.jaoow"
version = "1.9.1"

allprojects {
    plugins.apply("java")
    plugins.apply("maven-publish")

    repositories {
        mavenCentral()
        mavenLocal()
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                groupId = rootProject.group.toString()
                version = rootProject.version.toString()
            }
        }
    }

    dependencies {
        val lombokVersion = "1.18.22"
        val annotationsVersion = "21.0.1"

        val hikariVersion = "4.0.3"
        val sqliteVersion = "3.45.1.0"
        val mysqlVersion = "8.0.33"
        val mariaDBVersion = "3.3.2";

        // Lombok
        annotationProcessor("org.projectlombok:lombok:$lombokVersion")
        compileOnly("org.projectlombok:lombok:$lombokVersion")

        testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")
        testCompileOnly("org.projectlombok:lombok:$lombokVersion")

        // Jetbrains Annotations
        compileOnly("org.jetbrains:annotations-java5:$annotationsVersion")

        // HikariCP
        implementation("com.zaxxer:HikariCP:$hikariVersion")

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