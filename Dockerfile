# ==============================================================================
# STAGE 1: BUILD
# Usa Red Hat UBI 9 con OpenJDK 17 per compilare l'applicazione
# ==============================================================================
FROM registry.access.redhat.com/ubi9/openjdk-17:1.18 AS builder

# Metadata dell'immagine
LABEL stage=builder \
      description="Build stage per applicazione Spring Boot" \
      maintainer="your-email@example.com"

# Imposta directory di lavoro
WORKDIR /workspace/app

# Copia file Maven per dependency resolution
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Rendi eseguibile Maven wrapper
RUN chmod +x mvnw

# Scarica dipendenze (layer cacheable)
RUN ./mvnw dependency:go-offline -B

# Copia codice sorgente
COPY src src

# Compila l'applicazione
# -DskipTests per velocizzare (i test vanno eseguiti in CI/CD)
# -B per modalità batch (non interattiva)
RUN ./mvnw clean package -DskipTests -B

# Estrai layers per ottimizzare cache Docker
RUN mkdir -p target/dependency && \
    cd target/dependency && \
    jar -xf ../*.jar

# ==============================================================================
# STAGE 2: RUNTIME
# Usa Red Hat UBI 9 Runtime (più leggera, solo JRE)
# ==============================================================================
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:1.18

# Metadata dell'immagine finale
LABEL name="twelve-factor-demo" \
      version="1.0.0" \
      description="12-Factor Spring Boot Application con Red Hat UBI" \
      maintainer="your-email@example.com" \
      vendor="Your Company" \
      io.k8s.description="Cloud-native Spring Boot application" \
      io.k8s.display-name="12-Factor Demo" \
      io.openshift.tags="spring-boot,java,microservice"

# Crea directory per l'applicazione
WORKDIR /deployments

# Copia JAR buildato dallo stage precedente
# Usa layered approach per migliore caching
COPY --from=builder /workspace/app/target/dependency/BOOT-INF/lib /deployments/lib
COPY --from=builder /workspace/app/target/dependency/META-INF /deployments/META-INF
COPY --from=builder /workspace/app/target/dependency/BOOT-INF/classes /deployments

# Alternativa: copia direttamente il JAR (più semplice ma meno ottimizzato)
# COPY --from=builder /workspace/app/target/*.jar /deployments/app.jar

# Variabili di ambiente per configurazione
ENV JAVA_OPTS="-Xmx256m -Xms128m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SERVER_PORT=8080 \
    GREETING_PREFIX="Hello" \
    APP_VERSION="1.0.0" \
    SPRING_PROFILES_ACTIVE="prod"

# Esponi la porta dell'applicazione
EXPOSE 8080

# Health check per monitorare lo stato del container
HEALTHCHECK --interval=30s \
            --timeout=3s \
            --start-period=40s \
            --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Esegui come utente non-root per sicurezza (già impostato da UBI)
# USER 1001 è già il default in UBI runtime

# Entrypoint ottimizzato per layered JAR
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -cp /deployments:/deployments/lib/* it.alf.Application"]

# Alternativa per JAR standard (se usi la copia semplice)
# ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/app.jar"]