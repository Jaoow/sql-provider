plugins {
    id("java")
    id("maven")
}

group = "com.jaoow"
version = "1.0.0"

subprojects {

    plugins.apply("java")
    plugins.apply("maven")

    repositories {
        mavenCentral()
        jcenter()
    }

    dependencies {
        val lombokVersion = "1.18.22"
        val jetbrainsAnnotationsVersion = "21.0.1"

        compileOnly("org.projectlombok:lombok:$lombokVersion")
        compileOnly("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")

        annotationProcessor("org.projectlombok:lombok:$lombokVersion")
        annotationProcessor("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")

        testImplementation("org.xerial:sqlite-jdbc:3.36.0.2")
        testImplementation("mysql:mysql-connector-java:8.0.15")
        testImplementation("org.mariadb.jdbc:mariadb-java-client:2.4.2")
    }
}
