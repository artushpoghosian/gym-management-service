FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY gym-main-service/pom.xml gym-main-service/pom.xml
COPY trainer-workload-service/pom.xml trainer-workload-service/pom.xml
COPY integration-tests/pom.xml integration-tests/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl gym-main-service -am -DskipTests dependency:go-offline

COPY gym-main-service/src gym-main-service/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl gym-main-service -am -DskipTests package spring-boot:repackage

FROM eclipse-temurin:21-jre AS runtime

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/gym-main-service/target/gym-main-service-*.jar /app/app.jar
RUN chown -R 1000:1000 /app

USER 1000:1000
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
