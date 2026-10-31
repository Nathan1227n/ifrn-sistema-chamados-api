FROM maven:3.9.11-eclipse-temurin-21 AS compilacao
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV TZ=America/Fortaleza
RUN addgroup -S chamados && adduser -S chamados -G chamados
COPY --from=compilacao --chown=chamados:chamados /app/target/sistema-chamados-0.0.1-SNAPSHOT.jar app.jar
USER chamados
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
