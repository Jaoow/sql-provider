repositories {
    mavenCentral()
}

dependencies {
    val hikariVersion = "3.4.5"
    val slf4jVersion = "1.7.32"
    val sqliteVersion = "3.25.2"

    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
    implementation("org.xerial:sqlite-jdbc:${sqliteVersion}")
}
