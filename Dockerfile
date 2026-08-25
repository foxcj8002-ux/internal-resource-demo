FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY .mvn/docker-settings.xml /root/.m2/settings.xml
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
RUN useradd --system --uid 10001 --create-home --home-dir /home/app appuser
WORKDIR /app
COPY --from=build /workspace/target/internal-resource-demo-*.jar /app/app.jar
RUN mkdir -p /app/data && chown -R appuser:appuser /app
USER 10001
EXPOSE 8080
ENV SERVER_PORT=8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
