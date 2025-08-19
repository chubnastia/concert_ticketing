# ---- build stage (uses Gradle to build the jar) ----
FROM gradle:8.10-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle --no-daemon clean bootJar

# ---- run stage (small JRE to run the jar) ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*SNAPSHOT.jar /app/app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
