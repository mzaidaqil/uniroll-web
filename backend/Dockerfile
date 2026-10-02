# Stage 1: build the jar with the full JDK and Maven
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /app

# Copy only the build files first and download dependencies. Docker caches this layer,
# so changing Java code doesn't re-download every library on the next build.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline

COPY src/ src/
# Tests run separately (./mvnw test, and CI in Phase 7): they need Docker for Testcontainers
RUN ./mvnw -q package -DskipTests

# Stage 2: only a JRE and the jar. No JDK, Maven or source code in the image that runs.
FROM eclipse-temurin:21-jre-noble
WORKDIR /app

# Don't run as root inside the container
RUN useradd --system --uid 1001 spring
COPY --from=build /app/target/*.jar app.jar
USER spring

# Size the heap from the container's memory limit instead of the host's
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
