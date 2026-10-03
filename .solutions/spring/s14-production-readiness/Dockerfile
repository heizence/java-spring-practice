# The bookstore's container image, built from the jar that Gradle builds. From the repository root:
#
#   ./gradlew :spring:s14-production-readiness:bootJar
#   docker build -t bookstore spring/s14-production-readiness
#
# Two stages: the first unpacks the jar, the second is the image that runs. Only the second one ships.

# Stage 1: split the jar into layers. Dependencies change rarely, the application's own code changes with every
# commit; separate layers let Docker reuse the unchanged ones when it builds, pushes and pulls the image.
FROM eclipse-temurin:25-jre AS extract
WORKDIR /build
COPY build/libs/bookstore.jar bookstore.jar
RUN java -Djarmode=tools -jar bookstore.jar extract --layers --destination extracted

# Stage 2: the image that runs. A JRE, not a JDK: no compiler and no tools that nobody needs in production, and a
# smaller image with fewer things to patch.
FROM eclipse-temurin:25-jre

# Run as an ordinary user, never as root: if someone breaks into the application, they get no root rights in the
# container. A numeric id lets Kubernetes check "runAsNonRoot".
RUN groupadd --system --gid 10001 bookstore \
    && useradd --system --uid 10001 --gid bookstore --no-create-home bookstore

WORKDIR /app
# One COPY per layer, the layer that changes least first.
COPY --from=extract /build/extracted/dependencies/ ./
COPY --from=extract /build/extracted/spring-boot-loader/ ./
COPY --from=extract /build/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/extracted/application/ ./

USER 10001:10001
EXPOSE 8080

# Exec form (a JSON array): java runs as process 1 and receives the stop signal (SIGTERM) itself, so Spring Boot's
# graceful shutdown runs. The shell form would start a shell that doesn't pass the signal on.
# The JVM sees the container's memory limit; MaxRAMPercentage lets the heap use 75% of it (the default is 25%).
# Everything else (database URL, passwords, profiles) comes from environment variables when the container starts.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "bookstore.jar"]
