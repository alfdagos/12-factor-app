# ========================================
# 12-FACTOR APP - MULTI-STAGE DOCKERFILE
# ========================================
# Questo Dockerfile implementa:
# - FACTOR II: Dependencies (isolamento dipendenze)
# - FACTOR V: Build, Release, Run (separazione build/runtime)
# - FACTOR X: Dev/Prod Parity (stessa immagine per tutti gli ambienti)

# ========================================
# STAGE 1: BUILD
# ========================================
# FACTOR II: Dependencies - Ambiente isolato per build
# FACTOR V: Build stage - Compila codice in artefatto immutabile
FROM registry.access.redhat.com/ubi9/openjdk-21:1.20 AS builder

WORKDIR /workspace/app

# Copia file di build (pom.xml prima per cache layer)
COPY pom.xml .
COPY src ./src

# FACTOR II: Scarica dipendenze in ambiente isolato
# Questo layer viene cachato se pom.xml non cambia
RUN mvn dependency:go-offline -B

# FACTOR V: Build produce JAR immutabile
RUN mvn clean package -DskipTests

# ========================================
# STAGE 2: RUNTIME
# ========================================
# FACTOR V: Runtime stage - Solo ciò che serve per eseguire
# FACTOR X: Stessa immagine base per ambienti diversi
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.20

WORKDIR /deployments

# FACTOR II: Copia SOLO artefatto finale, non build tools
# FACTOR V: JAR immutabile dalla build stage
COPY --from=builder /workspace/app/target/*.jar app.jar

# FACTOR VII: Port Binding - Documenta porta esposta
# Porta configurabile tramite SERVER_PORT env var
EXPOSE 8080

# FACTOR III: Config - Variabili d'ambiente con defaults
# Questi valori possono essere sovrascritti a runtime
ENV SERVER_PORT=8080 \
    GREETING_PREFIX="Hello" \
    APP_VERSION="1.0.0"

# FACTOR IX: Disposability - Health check per startup veloce
# Verifica che l'app sia pronta (usato da Docker, preferire K8s probes)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

# FACTOR VII: Port Binding - L'app si avvia autonomamente
# FACTOR IX: Processo principale con PID 1 per signal handling corretto
ENTRYPOINT ["java", "-jar", "app.jar"]

# Note:
# - FACTOR I: Codebase - Questo Dockerfile è nel repository
# - FACTOR IV: Backing Services - DB URL configurabili via env vars
# - FACTOR VI: Processes - Container stateless, scalabile
# - FACTOR VIII: Concurrency - Ogni container è un processo indipendente
# - FACTOR XI: Logs - Java logga su stdout, Docker li raccoglie
# - FACTOR XII: Admin - Stessa immagine per app e admin tasks
