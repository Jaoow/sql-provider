plugins {
    id("java-library")
    id("maven-publish")
}

group = "com.jaoow"
version = "1.9.1"

allprojects {
    plugins.apply("java")
    plugins.apply("maven-publish")

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"

        sourceCompatibility = "8"
        targetCompatibility = "8"
    }

    repositories {
        mavenCentral()
        mavenLocal()
    }

    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
    }

    dependencies {
        // Lombok
        rootProject.libs.lombok.let {
            compileOnly(it)
            annotationProcessor(it)
            testAnnotationProcessor(it)
            testCompileOnly(it)
        }

        // jetbrains
        rootProject.libs.jetbrains.let {
            compileOnly(it)
            testCompileOnly(it)
        }
    }

    publishing {
        publications {
            create<MavenPublication>("${rootProject.name}-${this@allprojects.name}") {
                from(components["java"])

                groupId = rootProject.group.toString()
                version = rootProject.version.toString()
            }
        }
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