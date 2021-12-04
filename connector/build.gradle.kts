repositories {
    mavenCentral()
}

dependencies {
    val hikariVersion = "3.4.5"
    val slf4jVersion = "1.7.32"
    val sqliteVersion = "3.36.0.2"

    compileOnly("org.xerial:sqlite-jdbc:${sqliteVersion}")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
}
