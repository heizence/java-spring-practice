# The bookstore's container image, built from the jar that Gradle builds. From the repository root:
#
#   ./gradlew :spring:s14-production-readiness:bootJar
#   docker build -t bookstore spring/s14-production-readiness
#
# TODO: S14-03 — this image starts, but it isn't fit for production. The ticket in the README lists what to change.
FROM eclipse-temurin:25-jdk
COPY build/libs/bookstore.jar app.jar
ENTRYPOINT java -jar app.jar
