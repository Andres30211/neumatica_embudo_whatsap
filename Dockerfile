
# Etapa de compilación
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:21-jdk-alpine

RUN apk add --no-cache ffmpeg

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
