// Test setup shared by every exercise module (Part 1 and Part 2): JUnit Platform, the Mockito agent, the score
// file that ./score reads, and mutants.
//
// Besides the normal `test` task, every directory under src/mutants/<id>/ becomes a "mutant": a deliberately broken
// copy of one or more main classes. `mutationTest` runs the exercise's tests against each mutant; a mutant is
// "caught" when at least one test fails. src/mutants/<id>/mutant.properties describes it (see CLAUDE.md).
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.util.Properties

plugins {
    java
}

// The module's convention plugin adds the mockito-core dependency to this configuration.
val mockitoAgent = configurations.maybeCreate("mockitoAgent")

// `./score` passes -Pscore: failing tests are expected while practicing, so they must not stop the run.
val scoreMode = providers.gradleProperty("score").isPresent
// -Prerun runs tests again even when nothing changed (no up-to-date check, no build cache), to hunt flaky tests.
val forceRerun = providers.gradleProperty("rerun").isPresent

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
    if (forceRerun) {
        outputs.upToDateWhen { false }
        outputs.cacheIf { false }
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
    // Part 1 names a test package; Part 2 names test filter patterns (tests=...).
    val testPatterns = props.getProperty("tests")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
        ?: props.getProperty("package")?.trim()?.let { listOf("$it.*") }
        ?: throw GradleException("$dir/mutant.properties needs a 'package' or a 'tests' entry")
    val suffix = id.split('-', '_', '.').joinToString("") { part -> part.replaceFirstChar(Char::uppercaseChar) }

    val main = sourceSets["main"]
    val test = sourceSets["test"]
    val mutantSourceSet = sourceSets.create("mutant$suffix") {
        java.setSrcDirs(listOf(dir.resolve("java")))
        resources.setSrcDirs(listOf(dir.resolve("resources")))
        compileClasspath += main.output + main.compileClasspath
    }
    // Mutants of classes that use annotation processors (Lombok) need the same processors.
    configurations[mutantSourceSet.annotationProcessorConfigurationName]
        .extendsFrom(configurations[main.annotationProcessorConfigurationName])

    val mutantTest = tasks.register<Test>("mutationTest$suffix") {
        group = "verification"
        description = "Runs ${testPatterns.joinToString()} against mutant '$id'."
        testClassesDirs = test.output.classesDirs
        // The mutant's classes come first on the classpath, so they replace the real ones.
        classpath = mutantSourceSet.output + test.runtimeClasspath
        filter {
            testPatterns.forEach { includeTestsMatching(it) }
            isFailOnNoMatchingTests = false
        }
        ignoreFailures = true // a failing test is the goal here: it means the mutant was caught
        systemProperty("practice.mutant", id)
        writeScoreTo("score/mutants/$id.tsv")
    }
    mutationTest { dependsOn(mutantTest) }
}
