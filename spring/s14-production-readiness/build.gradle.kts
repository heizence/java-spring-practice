// S14 · Production readiness: request-id logging, Actuator health and metrics, graceful shutdown, packaging.
plugins {
    id("practice.spring-exercises")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-mysql")
    // /actuator/health, /actuator/info and /actuator/metrics, health groups and Micrometer.
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("com.mysql:mysql-connector-j")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mysql")
}

// The Dockerfile copies this jar. One fixed name, and no second "-plain" jar that a wildcard would also match.
tasks.bootJar {
    archiveFileName = "bookstore.jar"
}

tasks.jar {
    enabled = false
}
