// Conventions for Part 2 exercise modules (spring/sNN-*): a Spring Boot 4 application.
// Each module's own build.gradle.kts lists the starters it needs, so you can see what every module uses.
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import java.time.Duration

plugins {
    id("practice.java-base")
    id("org.springframework.boot")
    id("practice.exercise-testing")
}

// Lombok's annotation processor uses sun.misc.Unsafe, which JDK 25 warns about on every compile. The warning is
// harmless, so allow the access in the compiler process to keep build output readable.
tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
    options.forkOptions.jvmArgs = listOf("--sun-misc-unsafe-memory-access=allow")
}

tasks.withType<Test>().configureEach {
    // testkit's junit-platform.properties runs each test method in a thread of its own, so that an endless loop
    // fails with a timeout. Spring binds a test's transaction (and MockMvc requests) to the thread that started it:
    // on another thread, a @Transactional test would commit instead of rolling back. Keep test methods on the
    // calling thread; the 10-second timeout still applies after the method returns.
    systemProperty("junit.jupiter.execution.timeout.thread.mode.default", "SAME_THREAD")
    // Without a separate thread, an endless loop can't be interrupted per test, so the whole run gets a limit.
    timeout = Duration.ofMinutes(10)
    // Byte Buddy (used by Hibernate and Mockito) calls sun.misc.Unsafe, which JDK 25 warns about. Harmless.
    jvmArgs("--sun-misc-unsafe-memory-access=allow")
    // Testcontainers otherwise asks Docker Desktop's credential helper for registry logins before using an image,
    // which can stall for tens of seconds. The images used here are public, so no login is needed.
    environment("DOCKER_AUTH_CONFIG", """{"auths":{}}""")
}

dependencies {
    // Spring Boot's bill of materials decides the version of every Spring-managed library.
    val springBootBom = platform(SpringBootPlugin.BOM_COORDINATES)
    implementation(springBootBom)
    annotationProcessor(springBootBom)
    testAnnotationProcessor(springBootBom)
    developmentOnly(springBootBom)
    "mockitoAgent"(springBootBom)

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(project(":testkit"))
    "mockitoAgent"("org.mockito:mockito-core") { isTransitive = false }
}
