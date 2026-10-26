FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/fintech-exam-practice-1.0.0.jar app.jar
EXPOSE 8095
ENTRYPOINT ["java", "-jar", "app.jar"]
