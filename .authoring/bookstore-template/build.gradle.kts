// The bookstore template: the starting point every Part 2 module copies (maintainers only).
plugins {
    id("practice.spring-exercises")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-mysql")
    runtimeOnly("com.mysql:mysql-connector-j")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mysql")
}

tasks.withType<Test>().configureEach {
    // testkit's junit-platform.properties runs each test method in a thread of its own, so that an endless loop
    // fails with a timeout. Spring binds a test's transaction (and MockMvc requests) to the thread that started it:
    // on another thread, a @Transactional test would commit instead of rolling back. Keep test methods on the
    // calling thread; the 10-second timeout still applies.
    systemProperty("junit.jupiter.execution.timeout.thread.mode.default", "SAME_THREAD")
    // Byte Buddy (used by Hibernate and Mockito) calls sun.misc.Unsafe, which JDK 25 warns about. The warning is
    // harmless; allow the access to keep the test output readable, as build-logic does for the compiler.
    jvmArgs("--sun-misc-unsafe-memory-access=allow")
}
