FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S convoyia && adduser -S convoyia -G convoyia
COPY target/*.jar app.jar
USER convoyia
ENTRYPOINT ["java", "-jar", "app.jar"]
