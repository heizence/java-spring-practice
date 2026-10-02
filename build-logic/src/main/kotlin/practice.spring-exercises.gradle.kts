// Conventions for Part 2 exercise modules (spring/sNN-*): a Spring Boot 4 application.
// Each module's own build.gradle.kts lists the starters it needs, so you can see what every module uses.
import org.springframework.boot.gradle.plugin.SpringBootPlugin

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
