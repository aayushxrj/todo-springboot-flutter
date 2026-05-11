# Build Spring Boot app
FROM eclipse-temurin:26-jdk AS build

WORKDIR /workspace

# Ensure Gradle wrapper can unzip distributions
RUN apt-get update \
	&& apt-get install -y --no-install-recommends unzip \
	&& rm -rf /var/lib/apt/lists/*

# Copy build scripts first for caching
COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle ./gradle
RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon dependencies

# Copy source and build
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

# Run stage
FROM eclipse-temurin:26-jre

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
