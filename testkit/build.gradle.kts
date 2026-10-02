// Shared test infrastructure: the listener that records results for ./score, the score report itself,
// and small helpers used by exercise tests (Predict, Concurrently).
plugins {
    id("practice.java-base")
    `java-library`
}

dependencies {
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)
    api(libs.assertj.core)
    implementation(libs.junit.platform.launcher)
}

tasks.register<JavaExec>("scoreReport") {
    group = "verification"
    description = "Prints the score from the latest test results. Use ./score, which runs the tests first."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "practice.testkit.score.ScoreReport"
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    val repoRoot = rootDir.absolutePath
    val only = providers.gradleProperty("only").orElse("")
    val compileFailed = providers.gradleProperty("compileFailed").orElse("")
    val mode = providers.gradleProperty("mode").orElse("report")
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(repoRoot, "--only", only.get(), "--compile-failed", compileFailed.get(), "--mode", mode.get())
    })
}
