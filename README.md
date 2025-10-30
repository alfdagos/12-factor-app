# 12-Factor App Demo - Spring Boot Cloud-Native

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-green)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-blue)

## 📖 Overview

Applicazione di riferimento che dimostra l'implementazione completa dei **[12-Factor App](https://12factor.net/)** principles utilizzando **Spring Boot** in modalità **cloud-native**.

Il progetto evidenzia best practices per costruire applicazioni moderne, scalabili e pronte per ambienti containerizzati e Kubernetes. Ogni componente include commenti dettagliati che identificano i principi implementati.

---

## 🎯 I 12 Fattori Implementati

| Factor | Principio | Implementazione in questo progetto |
|--------|-----------|-----------------------------------|
| **I. Codebase** | Una codebase tracciata, molti deploy | Repository Git unico, deployment multipli via Docker/K8s |
| **II. Dependencies** | Dichiara ed isola le dipendenze | Maven (pom.xml) + Dockerfile multi-stage |
| **III. Config** | Configurazione tramite environment | application.yml con `${VAR:default}` + ConfigMap K8s |
| **IV. Backing Services** | Tratta i servizi come risorse | Preparato per DB/cache con URL configurabili |
| **V. Build, Release, Run** | Separa build e runtime | Dockerfile multi-stage + versioning immagini |
| **VI. Processes** | Processi stateless | Service senza stato in memoria, scalabile |
| **VII. Port Binding** | Esporta servizi via port binding | Tomcat embedded, self-contained |
| **VIII. Concurrency** | Scala tramite processi | Multiple replicas K8s, stateless design |
| **IX. Disposability** | Fast startup, graceful shutdown | Graceful shutdown + health checks |
| **X. Dev/Prod Parity** | Ambienti simili | Stessa immagine Docker per tutti gli ambienti |
| **XI. Logs** | Log come stream di eventi | Log su stdout/stderr, nessun file |
| **XII. Admin Processes** | Task admin come one-off | Spring Actuator endpoints + K8s Jobs |

---

## 🏗️ Architettura del Progetto

```
twelve-factor-app/
├── src/
│   ├── main/
│   │   ├── java/it/alf/twelve_factor/
│   │   │   ├── Application.java          # [Factor I] Entry point
│   │   │   ├── controller/
│   │   │   │   └── GreetingController.java  # [Factor VII] Port binding
│   │   │   ├── service/
│   │   │   │   └── GreetingService.java     # [Factor VI] Stateless
│   │   │   └── model/
│   │   │       └── Greeting.java
│   │   └── resources/
│   │       └── application.yml           # [Factor III] Config
│   └── test/                             # Test unitari
├── k8s/                                  # [Factor VIII, IX] Kubernetes manifests
│   ├── deployment.yaml                   # Deployment + health checks
│   ├── service.yaml                      # Service
│   ├── configmap.yaml                    # [Factor III] Configurazione
│   ├── secret.yaml                       # Secrets (esempio)
│   └── ingress.yaml                      # Ingress
├── Dockerfile                            # [Factor II, V, X] Multi-stage build
├── pom.xml                               # [Factor II] Dipendenze
└── README.md                             # Questo file
```

---

## 🚀 Quick Start

### Prerequisiti

- **Java 21** (o superiore)
- **Maven 3.8+**
- **Docker** (opzionale, per containerizzazione)
- **Kubernetes** / **kubectl** (opzionale, per deploy)

### 1. Build Locale

```bash
# Compila il progetto
mvn clean package

# Esegui localmente
java -jar target/twelve-factor-demo-1.0.0.jar

# Oppure via Maven
mvn spring-boot:run
```

L'applicazione sarà disponibile su `http://localhost:8080`

### 2. Test dell'API

```bash
# Endpoint principale
curl http://localhost:8080/api/v1/greeting?name=Cloud

# Risposta attesa:
# {"id":1,"message":"Hello, Cloud!","version":"1.0.0"}

# Health check (Factor XII)
curl http://localhost:8080/actuator/health

# Metriche Prometheus (Factor XII)
curl http://localhost:8080/actuator/prometheus
```

### 3. Build Docker (Factor V, X)

```bash
# Build immagine
docker build -t twelve-factor-demo:1.0.0 .

# Run con environment variables (Factor III)
docker run -p 8080:8080 \
  -e GREETING_PREFIX="Ciao" \
  -e APP_VERSION="1.0.0-docker" \
  twelve-factor-demo:1.0.0

# Test
curl http://localhost:8080/api/v1/greeting?name=Docker
# {"id":1,"message":"Ciao, Docker!","version":"1.0.0-docker"}
```

### 4. Deploy su Kubernetes (Factor VIII, IX, X)

```bash
# Crea namespace (opzionale)
kubectl create namespace twelve-factor

# Applica ConfigMap (Factor III)
kubectl apply -f k8s/configmap.yaml

# Applica Secret (Factor III) - se necessario
kubectl apply -f k8s/secret.yaml

# Deploy applicazione
kubectl apply -f k8s/deployment.yaml

# Esponi il servizio
kubectl apply -f k8s/service.yaml

# (Opzionale) Ingress
kubectl apply -f k8s/ingress.yaml

# Verifica status
kubectl get pods
kubectl get services

# Vedi i logs (Factor XI)
kubectl logs -f deployment/twelve-factor-demo

# Scala orizzontalmente (Factor VIII)
kubectl scale deployment/twelve-factor-demo --replicas=5
```

---

## 📚 Implementazione dei 12 Fattori

### Factor I: Codebase
**Dove:** Repository Git, `.gitignore`

Un'unica codebase per tutti gli ambienti (dev, staging, production). La differenza sta nella **configurazione**, non nel codice.

```bash
# Stesso codice, configurazioni diverse
git clone <repo>
# Deploy dev con config-dev.yml
# Deploy prod con config-prod.yml
```

### Factor II: Dependencies
**Dove:** `pom.xml`, `Dockerfile`

Tutte le dipendenze sono dichiarate esplicitamente in `pom.xml`. Il Dockerfile multi-stage garantisce build riproducibili.

```xml
<!-- pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <!-- Versione gestita dal parent -->
    </dependency>
</dependencies>
```

### Factor III: Config
**Dove:** `application.yml`, `k8s/configmap.yaml`

La configurazione è esternalizzata tramite environment variables. Nessun valore sensibile nel codice.

```yaml
# application.yml
app:
  greeting:
    prefix: ${GREETING_PREFIX:Hello}  # Valore da env o default "Hello"
```

```java
// GreetingService.java
@Value("${app.greeting.prefix:Hello}")
private String greetingPrefix;  // ✅ Factor III
```

### Factor IV: Backing Services
**Dove:** `application.yml` (preparato per DB/cache)

Database, cache, code messaggi sono risorse collegate tramite URL configurabili.

```yaml
# Esempio di backing service (non ancora implementato)
spring:
  datasource:
    url: ${DATABASE_URL}        # ✅ Configurabile via env
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### Factor V: Build, Release, Run
**Dove:** `Dockerfile`, CI/CD pipeline

**Build** → JAR immutabile  
**Release** → JAR + Config = Immagine Docker versionata  
**Run** → Kubernetes esegue l'immagine

```dockerfile
# Stage BUILD
FROM openjdk-21 AS builder
RUN mvn clean package  # ✅ Produce JAR

# Stage RUNTIME
FROM openjdk-21-runtime
COPY --from=builder target/*.jar app.jar  # ✅ JAR immutabile
```

### Factor VI: Processes
**Dove:** `GreetingService.java`

L'applicazione è **stateless**. Nessuna sessione in memoria. Ogni richiesta è indipendente.

```java
@Service
public class GreetingService {
    // ✅ Factor VI: Counter locale è accettabile per metriche
    // NO stato di sessione utente
    private final AtomicLong counter = new AtomicLong();
    
    public Greeting createGreeting(String name) {
        // Ogni richiesta è indipendente ✅
        return new Greeting(counter.incrementAndGet(), message, version);
    }
}
```

### Factor VII: Port Binding
**Dove:** `Application.java`, `pom.xml`

L'app include il web server (Tomcat embedded). È self-contained, non richiede server esterno.

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);  // ✅ Tomcat embedded
    }
}
```

```yaml
server:
  port: ${SERVER_PORT:8080}  # ✅ Porta configurabile
```

### Factor VIII: Concurrency
**Dove:** `k8s/deployment.yaml`

Scalabilità orizzontale: più istanze identiche dello stesso processo.

```yaml
spec:
  replicas: 2  # ✅ 2 pod identici
```

```bash
# Scala a 5 istanze
kubectl scale deployment/twelve-factor-demo --replicas=5
```

### Factor IX: Disposability
**Dove:** `application.yml`, `k8s/deployment.yaml`

**Fast startup**: l'app si avvia in pochi secondi  
**Graceful shutdown**: completa le richieste prima di terminare

```yaml
# application.yml
server:
  shutdown: graceful  # ✅ Arresto pulito

# deployment.yaml
livenessProbe:
  httpGet:
    path: /actuator/health
  initialDelaySeconds: 30  # ✅ Tempo per startup
```

### Factor X: Dev/Prod Parity
**Dove:** `Dockerfile`, Docker Compose (da aggiungere)

Stessa immagine Docker in tutti gli ambienti. Solo la **config** cambia.

```bash
# Dev
docker run -e GREETING_PREFIX="Dev" app:1.0.0

# Prod
docker run -e GREETING_PREFIX="Production" app:1.0.0
# ✅ Stessa immagine, config diversa
```

### Factor XI: Logs
**Dove:** `GreetingService.java`, `application.yml`

I log vanno su **stdout/stderr**, non su file. Kubernetes li raccoglie automaticamente.

```java
@Slf4j
public class GreetingService {
    public Greeting createGreeting(String name) {
        log.info("Greeting created: {}", message);  // ✅ Stdout
        return greeting;
    }
}
```

```yaml
# application.yml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"  # ✅ Stdout
```

```bash
# Vedi log in Kubernetes
kubectl logs -f deployment/twelve-factor-demo
```

### Factor XII: Admin Processes
**Dove:** `pom.xml` (Actuator), `k8s/` (Jobs)

Task amministrativi (migration, backup) vengono eseguiti come processi one-off con la stessa immagine.

```yaml
# Endpoints amministrativi
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus  # ✅ Admin endpoints
```

```bash
# Usa gli admin endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/prometheus

# One-off task con Kubernetes Job
kubectl run migration --rm -it --image=twelve-factor-demo:1.0.0 --restart=Never -- java -jar app.jar --migrate
```

---

## 🔧 Configurazione

### Environment Variables (Factor III)

| Variabile | Default | Descrizione |
|-----------|---------|-------------|
| `SERVER_PORT` | `8080` | Porta del server |
| `GREETING_PREFIX` | `Hello` | Prefisso del messaggio |
| `APP_VERSION` | `1.0.0` | Versione applicazione |
| `SPRING_PROFILES_ACTIVE` | - | Profilo Spring attivo |

### Esempio ConfigMap Kubernetes

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: twelve-factor-demo-config
data:
  SERVER_PORT: "8080"
  GREETING_PREFIX: "Hello from Kubernetes"
  APP_VERSION: "1.0.0"
```

---

## 🧪 Test

```bash
# Esegui test unitari
mvn test

# Esegui test con coverage
mvn verify

# Test di integrazione
mvn verify -P integration-tests
```

---

## 📊 Monitoring e Observability (Factor XII)

### Health Checks

```bash
# Liveness (è vivo?)
curl http://localhost:8080/actuator/health/liveness

# Readiness (è pronto a ricevere traffico?)
curl http://localhost:8080/actuator/health/readiness
```

### Metriche Prometheus

```bash
# Tutte le metriche in formato Prometheus
curl http://localhost:8080/actuator/prometheus

# Metriche JVM, HTTP requests, custom metrics
```

### Grafana Dashboard

Importa le metriche Prometheus in Grafana per visualizzazioni avanzate.

---

## 🐳 Comandi Docker Utili

```bash
# Build
docker build -t twelve-factor-demo:1.0.0 .

# Run locale con env vars
docker run -p 8080:8080 \
  -e GREETING_PREFIX="Hello Docker" \
  -e APP_VERSION="1.0.0" \
  twelve-factor-demo:1.0.0

# Run in background
docker run -d -p 8080:8080 --name twelve-factor twelve-factor-demo:1.0.0

# Vedi logs (Factor XI)
docker logs -f twelve-factor

# Stop
docker stop twelve-factor

# Remove
docker rm twelve-factor

# Build multi-platform (per ARM e x86)
docker buildx build --platform linux/amd64,linux/arm64 -t twelve-factor-demo:1.0.0 .
```

---

## ☸️ Comandi Kubernetes Utili

```bash
# Deploy completo
kubectl apply -f k8s/

# Vedi status
kubectl get all

# Scala deployment (Factor VIII)
kubectl scale deployment/twelve-factor-demo --replicas=3

# Vedi logs di tutti i pod (Factor XI)
kubectl logs -l app=twelve-factor-demo --tail=100 -f

# Port forward per test locale
kubectl port-forward service/twelve-factor-demo-service 8080:8080

# Debugging
kubectl describe pod <pod-name>
kubectl exec -it <pod-name> -- /bin/bash

# Rollback
kubectl rollout undo deployment/twelve-factor-demo

# History
kubectl rollout history deployment/twelve-factor-demo
```

---

## 📖 Riferimenti e Approfondimenti

- [The Twelve-Factor App](https://12factor.net/) - Metodologia originale
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
- [Red Hat Universal Base Images](https://catalog.redhat.com/software/containers/search)

---

## 🤝 Contribuire

Pull request benvenute per:
- Migliorare i commenti esplicativi
- Aggiungere test
- Migliorare la documentazione
- Aggiungere esempi di backing services (DB, cache)

---

## 📝 Licenza

Progetto open source.

---

## 👨‍💻 Autore

Progetto di riferimento per l'implementazione dei principi cloud-native e dei 12-Factor App.
