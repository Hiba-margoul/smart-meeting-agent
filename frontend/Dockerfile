# Étape 1 : Construction (Build)
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Compile le projet et crée le .jar
RUN mvn clean package -DskipTests

# Étape 2 : Exécution (Run)
FROM openjdk:17-jdk-slim
WORKDIR /app
# Récupère le .jar créé à l'étape précédente
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]