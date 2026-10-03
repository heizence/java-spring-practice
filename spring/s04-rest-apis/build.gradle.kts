// S04 · REST APIs: the bookstore's web layer, without a database (the catalog lives in memory).
plugins {
    id("practice.spring-exercises")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // OpenAPI documentation (S04-09). Not managed by Spring Boot's BOM: springdoc-openapi 3.x supports Spring Boot 4.
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
}
