# Stage 1: Build Environment (named "builder")
FROM eclipse-temurin:21.0.8_9-jdk-jammy AS builder
WORKDIR /opt/app

# Copy Maven wrapper and pom.xml to download dependencies first
# This leverages Docker's layer caching for faster subsequent builds
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline

# Copy source code and build the application
COPY ./src ./src
RUN ./mvnw clean install -DskipTests

# Stage 2: Runtime Environment (named "final")
FROM eclipse-temurin:21.0.8_9-jre-jammy AS final
WORKDIR /opt/app

# Expose the application port
EXPOSE 8083

# Copy only the compiled JAR from the builder stage
# This keeps the final image lean as it won't contain Maven or the full JDK
COPY --from=builder /opt/app/target/*.jar /opt/app/*.jar

# Define the command to run the application
ENTRYPOINT ["java", "-jar", "/opt/app/*.jar"]
