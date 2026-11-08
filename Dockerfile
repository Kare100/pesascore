FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/pesascore.jar .
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "pesascore.jar", "serve", "--port", "8080"]
