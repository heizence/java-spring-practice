// Conventions shared by every Java project in this repository.
plugins {
    java
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.findVersion("java").get().requiredVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // -parameters keeps constructor/method parameter names for reflection (J09, and Spring later).
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:unchecked,rawtypes,deprecation"))
}
