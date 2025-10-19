# Multi-stage build
FROM registry.access.redhat.com/ubi9/openjdk-21:1.20 AS builder

WORKDIR /workspace/app

# Copia i file di build
COPY pom.xml .
COPY src ./src

# Scarica dipendenze e compila
RUN mvn dependency:go-offline -B
RUN mvn clean package -DskipTests

# Runtime stage
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.20

WORKDIR /deployments

# Copia SOLO il JAR dalla stage di build
COPY --from=builder /workspace/app/target/*.jar app.jar

# Espone la porta
EXPOSE 8080

# Comando di avvio
ENTRYPOINT ["java", "-jar", "app.jar"]