// Conventions for Part 1 exercise modules (java/jNN-*): plain Java 25 with JUnit 6, AssertJ and Mockito.
plugins {
    id("practice.java-base")
    id("practice.exercise-testing")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
    testImplementation(libs.findLibrary("assertj-core").get())
    testImplementation(libs.findLibrary("mockito-core").get())
    testImplementation(libs.findLibrary("mockito-junit-jupiter").get())
    testImplementation(project(":testkit"))
    "mockitoAgent"(libs.findLibrary("mockito-core").get()) { isTransitive = false }
}
