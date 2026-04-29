FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre

WORKDIR /opt/omnistack

ENV TZ=America/Guayaquil
ENV SPRING_PROFILES_ACTIVE=dev
ENV SERVER_PORT=8085

COPY --from=build /app/target/omnistack-backend-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/opt/omnistack/app.jar"]
