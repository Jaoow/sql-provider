version = rootProject.version

dependencies {
    rootProject.libs.let {
        implementation(it.hikari)
        implementation(it.sqlite)
        implementation(it.mysql)
        implementation(it.mariadb)
    }
}