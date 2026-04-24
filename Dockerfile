FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN wget -O /app/elastic-apm-agent.jar https://search.maven.org/remotecontent?filepath=co/elastic/apm/elastic-apm-agent/1.49.0/elastic-apm-agent-1.49.0.jar

COPY --from=build /app/target/mlb-strikeout-predictor-api-0.1.0.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-javaagent:/app/elastic-apm-agent.jar", "-jar", "/app/app.jar"]
