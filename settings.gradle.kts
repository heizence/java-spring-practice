pluginManagement {
    // Shared build conventions (toolchain, test setup, scoring, mutants) live in build-logic/.
    includeBuild("build-logic")
}

plugins {
    // Downloads the JDK version requested by the toolchain if it isn't installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "java-spring-practice"

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

// Test helpers and the score report used by every exercise module.
include("testkit")

// Part 1 — Java
listOf(
    "j01-basics",
    "j02-oop",
    "j03-exceptions-testing",
    "j04-generics",
    "j05-collections",
    "j06-functional",
    "j07-everyday-apis",
    "j08-concurrency",
    "j09-reflection-mini-spring",
    "j10-jvm",
).forEach { module -> include("java:$module") }

// Part 2 — Spring Boot (added in stage 2)
