FROM maven:3.9.5-eclipse-temurin-17 AS build-stage
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre AS runtime-stage
WORKDIR /app

# Install required packages
RUN apt-get update && apt-get install -y bash curl && rm -rf /var/lib/apt/lists/*

# Create certificates directory
RUN mkdir -p certificates

COPY --from=build-stage /app/target/*.jar SSO-0.0.1-SNAPSHOT.jar
COPY --from=build-stage /app/src/main/resources/certificates/ ./certificates/

EXPOSE 2080

ENTRYPOINT ["java", "-jar", "SSO-0.0.1-SNAPSHOT.jar"]
