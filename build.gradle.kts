group = rootProject.property("group")!!
version = rootProject.property("version")!!

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        val versions = contextVersions("version")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${versions["kotlin"]}")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:${versions["kt-lint"]}")
    }
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
    for (childProject in childProjects.values) {
        delete(childProject.buildDir)
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint") // Version should be inherited from parent
    configurations.all {
        val versions = contextVersions("version")
        if (!name.startsWith("ktlint")) {
            resolutionStrategy {
                eachDependency {
                    // Force Kotlin to our version
                    if (requested.group == "org.jetbrains.kotlin") {
                        useVersion(versions["kotlin"])
                    }
                }
            }
        }
    }
}
