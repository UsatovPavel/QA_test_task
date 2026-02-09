# Этап 1: Сборка
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Используем BuildKit кэш для зависимостей
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -B

# Этап 2: Финальный образ (сразу JDK, чтобы были jstack и jstat)
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Копируем готовый JAR
COPY --from=build /app/target/internal-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Dsecret=qazWSXedc", "-Dmock=http://wiremock:8080", "-jar", "app.jar"]
