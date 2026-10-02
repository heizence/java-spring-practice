// Conventions for exercise modules (java/jNN-*).
//
// Besides the normal `test` task, every directory under src/mutants/<id>/ becomes a "mutant":
// a deliberately broken copy of one or more main classes. `mutationTest` runs the exercise's
// tests against each mutant; a mutant is "caught" when at least one test fails.
// src/mutants/<id>/mutant.properties describes it (package, audience, description, hint).
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.util.Properties

plugins {
    id("practice.java-base")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
    testImplementation(libs.findLibrary("assertj-core").get())
    testImplementation(libs.findLibrary("mockito-core").get())
    testImplementation(libs.findLibrary("mockito-junit-jupiter").get())
    testImplementation(project(":testkit"))
    mockitoAgent(libs.findLibrary("mockito-core").get()) { isTransitive = false }
}

// `./score` passes -Pscore: failing tests are expected while practicing, so they must not stop the run.
val scoreMode = providers.gradleProperty("score").isPresent

fun Test.writeScoreTo(relativePath: String) {
    val scoreFile = layout.buildDirectory.file(relativePath)
    outputs.file(scoreFile).withPropertyName("scoreFile")
    systemProperty("practice.score.file", scoreFile.get().asFile.absolutePath)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = 1
    failOnNoDiscoveredTests = false
    // Load Mockito as an agent instead of letting it self-attach at runtime (avoids JDK warnings).
    val agent = mockitoAgent
    jvmArgumentProviders.add(CommandLineArgumentProvider { listOf("-javaagent:${agent.asPath}") })
    testLogging {
        events("failed")
        exceptionFormat = TestExceptionFormat.SHORT
    }
}

tasks.named<Test>("test") {
    ignoreFailures = scoreMode
    writeScoreTo("score/test.tsv")
}

val mutationTest = tasks.register("mutationTest") {
    group = "verification"
    description = "Runs the exercise tests against every deliberately broken version in src/mutants."
}

val mutantsRoot = layout.projectDirectory.dir("src/mutants").asFile
mutantsRoot.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name }?.forEach { dir ->
    val id = dir.name
    val props = Properties().apply { dir.resolve("mutant.properties").reader().use { load(it) } }
    val packageName = props.getProperty("package")
        ?: throw GradleException("$dir/mutant.properties needs a 'package' entry")
    val suffix = id.split('-', '_', '.').joinToString("") { part -> part.replaceFirstChar(Char::uppercaseChar) }

    val main = sourceSets["main"]
    val test = sourceSets["test"]
    val mutantSourceSet = sourceSets.create("mutant$suffix") {
        java.setSrcDirs(listOf(dir.resolve("java")))
        resources.setSrcDirs(emptyList<File>())
        compileClasspath += main.output + main.compileClasspath
    }

    val mutantTest = tasks.register<Test>("mutationTest$suffix") {
        group = "verification"
        description = "Runs the tests in $packageName against mutant '$id'."
        testClassesDirs = test.output.classesDirs
        // The mutant's classes come first on the classpath, so they replace the real ones.
        classpath = mutantSourceSet.output + test.runtimeClasspath
        filter {
            includeTestsMatching("$packageName.*")
            isFailOnNoMatchingTests = false
        }
        ignoreFailures = true // a failing test is the goal here: it means the mutant was caught
        systemProperty("practice.mutant", id)
        writeScoreTo("score/mutants/$id.tsv")
    }
    mutationTest { dependsOn(mutantTest) }
}
