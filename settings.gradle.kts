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

// Part 2 — Spring Boot. A module is included once its folder exists.
listOf(
    "s01-boot-fundamentals",
    "s02-configuration",
    "s03-aop",
    "s04-rest-apis",
    "s05-database-foundations",
    "s06-spring-data-jpa",
    "s07-transactions",
    "s08-concurrency-integrity",
    "s09-events-async",
    "s10-caching",
    "s11-http-clients",
    "s12-security",
    "s13-testing-strategy",
    "s14-production-readiness",
).filter { module -> file("spring/$module/build.gradle.kts").exists() }
    .forEach { module -> include("spring:$module") }

// Maintainers only: the bookstore template that Part 2 modules are copied from (exists only on `authoring`).
if (file(".authoring/bookstore-template/build.gradle.kts").exists()) {
    include("bookstore-template")
    project(":bookstore-template").projectDir = file(".authoring/bookstore-template")
}
