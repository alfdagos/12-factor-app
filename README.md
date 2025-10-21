# Guida Completa: Implementazione dei 12-Factor App

## 🎯 Panoramica
Questa guida spiega ogni singolo intervento nel codice per garantire la compliance con i 12-Factor App principles.

---

## Factor I: Codebase
**Principio**: *Una codebase tracciata nel version control, molti deploy*

### Implementazione nel Codice

```java
// Struttura del progetto
twelve-factor-demo/
├── src/
├── pom.xml
├── Dockerfile
├── .gitignore        // ✅ CHIAVE per Factor I
└── README.md
```

### Interventi Specifici

**1. File `.gitignore`**
```
# Build artifacts
target/
*.jar
*.war
*.ear

# Environment variables
.env
.env.local

# IDE
.vscode/
.idea/
*.iml

# Logs
*.log
logs/

# OS
.DS_Store
Thumbs.db           # Esclude configurazioni locali
```

**Perché è importante**: 
- Solo il **codice sorgente** va in Git, non gli artefatti
- Ogni environment (dev/staging/prod) usa la stessa codebase
- Il JAR viene generato dal CI/CD, non committato

**Verifica**:
```bash
git status  # Non deve mostrare file .jar o target/
```

---

## Factor II: Dependencies
**Principio**: *Dichiara esplicitamente e isola le dipendenze*

### Implementazione nel Codice

**1. File `pom.xml` - Dichiarazione Esplicita**
```xml
<dependencies>
    <!-- ✅ Versioni esplicite (ereditate da parent) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <!-- NO versione hardcoded, gestita da BOM -->
    </dependency>
</dependencies>

<!-- ✅ Parent POM per dependency management -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>  <!-- Versione esplicita -->
</parent>
```

**2. Dockerfile - Isolamento delle Dipendenze**
```dockerfile
# ✅ Stage BUILD - Scarica dipendenze in ambiente isolato
FROM registry.access.redhat.com/ubi9/openjdk-17:1.18 AS builder
COPY pom.xml .
RUN mvn dependency:go-offline  # Scarica tutte le dipendenze

# ✅ Stage RUNTIME - Solo dipendenze runtime
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:1.18
COPY --from=builder /workspace/app/target/*.jar app.jar
```

**Perché è importante**:
- **Nessuna dipendenza implicita** dal sistema operativo
- **Riproducibilità**: stessa build su qualsiasi macchina
- **Isolamento**: container garantisce ambiente pulito

**Verifica**:
```bash
# Deve funzionare su qualsiasi sistema con Docker
docker build -t test .
docker run test
```

---

## Factor III: Config
**Principio**: *Memorizza la configurazione nell'environment*

### Implementazione nel Codice

**1. File `application.yml` - Template con Defaults**
```yaml
server:
  port: ${SERVER_PORT:8080}  # ✅ Env var con fallback

app:
  greeting:
    prefix: ${GREETING_PREFIX:Hello}  # ✅ Config esternalizzata
  version: ${APP_VERSION:1.0.0}
```

**Anatomia della sintassi**:
- `${VARIABLE}`: legge da environment
- `:default`: valore di fallback se variabile non esiste
- **MAI** hardcodare secrets o config specifiche dell'ambiente

**2. Service Class - Injection della Config**
```java
@Service
public class GreetingService {
    
    // ✅ Inietta config da environment
    @Value("${app.greeting.prefix:Hello}")
    private String greetingPrefix;
    
    // ❌ SBAGLIATO: private String prefix = "Hello";
}
```

**3. Dockerfile - Espone Environment Variables**
```dockerfile
# ✅ Documenta le variabili configurabili
ENV SERVER_PORT=8080 \
    GREETING_PREFIX="Hello" \
    APP_VERSION="1.0.0"
```

**4. Kubernetes ConfigMap - Separazione Config**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  greeting.prefix: "Hello"  # ✅ Config fuori dal codice
```

**Perché è importante**:
- Stesso container per **tutti gli ambienti**
- Config diversa: dev usa "Hello", prod usa "Benvenuto"
- **Zero ricompilazione** per cambiare environment

**Verifica**:
```bash
# Stesso container, config diversa
docker run -e GREETING_PREFIX="Dev" app:1.0  # Development
docker run -e GREETING_PREFIX="Prod" app:1.0 # Production
```

---

## Factor IV: Backing Services
**Principio**: *Tratta i backing services come risorse collegate*

### Implementazione nel Codice

**1. Preparazione per Backing Services**
```yaml
# application.yml
spring:
  datasource:
    url: ${DATABASE_URL}      # ✅ URL configurabile
    username: ${DB_USERNAME}   # ✅ Credentials da env
    password: ${DB_PASSWORD}

  redis:
    host: ${REDIS_HOST:localhost}  # ✅ Cache service
    port: ${REDIS_PORT:6379}
```

**2. Service Interface - Astrazione**
```java
// ✅ Interface per backing service
public interface CacheService {
    void store(String key, String value);
    String retrieve(String key);
}

// Implementazione con Redis
@Service
@ConditionalOnProperty(name = "cache.type", havingValue = "redis")
public class RedisCacheService implements CacheService {
    @Autowired
    private RedisTemplate<String, String> redis;
    
    @Override
    public void store(String key, String value) {
        redis.opsForValue().set(key, value);
    }
}

// Implementazione con In-Memory (dev)
@Service
@ConditionalOnProperty(name = "cache.type", havingValue = "memory")
public class MemoryCacheService implements CacheService {
    private Map<String, String> cache = new ConcurrentHashMap<>();
    
    @Override
    public void store(String key, String value) {
        cache.put(key, value);
    }
}
```

**Perché è importante**:
- Puoi **switchare** da Redis locale a Redis AWS con 1 variabile
- Database di dev ≠ database di prod, ma stesso codice
- Backing service guasto? Cambi URL, riavvii, risolto

**Verifica**:
```bash
# Dev: usa database locale
DATABASE_URL=jdbc:postgresql://localhost:5432/devdb

# Prod: usa RDS AWS
DATABASE_URL=jdbc:postgresql://prod-rds.aws.com:5432/proddb
```

---

## Factor V: Build, Release, Run
**Principio**: *Separa rigorosamente gli stage di build e run*

### Implementazione nel Codice

**1. Dockerfile Multi-Stage - Separazione Build/Run**
```dockerfile
# ========================================
# STAGE 1: BUILD
# ========================================
FROM registry.access.redhat.com/ubi9/openjdk-17:1.18 AS builder

WORKDIR /workspace/app
COPY pom.xml .
COPY src src

# ✅ Build produce un artefatto immutabile
RUN mvn clean package -DskipTests

# ========================================
# STAGE 2: RELEASE (immagine finale)
# ========================================
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:1.18

# ✅ Solo runtime, no build tools
COPY --from=builder /workspace/app/target/*.jar app.jar

# ========================================
# STAGE 3: RUN (a runtime)
# ========================================
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**2. CI/CD Pipeline (esempio GitLab CI)**
```yaml
# .gitlab-ci.yml
stages:
  - build      # ✅ Factor V: Build stage
  - release    # ✅ Factor V: Release stage
  - deploy     # ✅ Factor V: Run stage

build:
  stage: build
  script:
    - mvn clean package
  artifacts:
    paths:
      - target/*.jar

release:
  stage: release
  script:
    - docker build -t app:${CI_COMMIT_SHA} .
    - docker tag app:${CI_COMMIT_SHA} app:latest
    - docker push app:${CI_COMMIT_SHA}

deploy-prod:
  stage: deploy
  script:
    - kubectl set image deployment/app app=app:${CI_COMMIT_SHA}
  only:
    - main
```

**Separazione degli Stage**:

| Stage | Cosa Produce | Dove Avviene | Ripetibile? |
|-------|-------------|--------------|-------------|
| **Build** | `app.jar` | CI/CD server | Sì, sempre uguale |
| **Release** | `app:v1.2.3` (immagine Docker) | Docker registry | Sì, immagine immutabile |
| **Run** | Container in esecuzione | Kubernetes/Prod | Sì, rollback possibile |

**Perché è importante**:
- **Build** = 1 volta, **Run** = 1000 volte
- Rollback istantaneo: `kubectl rollout undo`
- Audit trail: ogni release è tracciata

**Verifica**:
```bash
# Build produce SHA immutabile
docker build -t app:abc123 .

# Deploy usa SHA specifico
kubectl set image deployment/app app=app:abc123

# Rollback al deploy precedente
kubectl rollout undo deployment/app
```

---

## Factor VI: Processes
**Principio**: *Esegui l'app come uno o più processi stateless*

### Implementazione nel Codice

**1. Service Stateless - NO Session State**
```java
@Service
public class GreetingService {
    
    // ✅ CORRETTO: Counter in memoria NON persistente
    // Ogni pod ha il suo counter (accettabile per metriche)
    private final AtomicLong counter = new AtomicLong();
    
    // ❌ SBAGLIATO: Sessione utente in memoria
    // private Map<String, UserSession> sessions = new HashMap<>();
    
    public Greeting createGreeting(String name) {
        long count = counter.incrementAndGet();
        
        // ✅ Ogni richiesta è indipendente
        String message = String.format("%s, %s!", greetingPrefix, name);
        
        return new Greeting(count, message, appVersion);
    }
}
```

**2. Controller Stateless**
```java
@RestController
@RequiredArgsConstructor  // ✅ Dependency injection, no stato
public class GreetingController {
    
    private final GreetingService greetingService;
    
    // ✅ CORRETTO: Metodo puro, no side effects
    @GetMapping("/greeting")
    public Greeting getGreeting(@RequestParam String name) {
        return greetingService.createGreeting(name);
    }
    
    // ❌ SBAGLIATO: Stato mutabile nel controller
    // private List<String> recentUsers = new ArrayList<>();
}
```

**3. Gestione Sessioni - Esternalizzata**
```java
// ✅ Se serve sessione, usa backing service
@Configuration
public class SessionConfig {
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Sessioni in Redis, non in memoria del pod
        return new LettuceConnectionFactory(redisHost, redisPort);
    }
}
```

**4. Kubernetes Deployment - Multiple Replicas**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: twelve-factor-app
spec:
  replicas: 3  # ✅ Possibile solo se stateless
  template:
    spec:
      containers:
      - name: app
        image: twelve-factor-demo:1.0.0
        # ✅ Ogni pod è intercambiabile
```

**Cosa significa "Stateless"**:

| ✅ Permesso | ❌ Vietato |
|------------|-----------|
| Config caricata all'avvio | Sessioni utente in memoria |
| Cache locale read-through | File caricati su disco locale |
| Metriche in-memory (counter) | Stato condiviso tra richieste |
| Connection pool | Lock distribuiti in memoria |

**Perché è importante**:
- **Scalabilità**: posso avere 10 pod identici
- **Resilienza**: pod muore? Nessun dato perso
- **Load balancing**: qualsiasi pod può gestire la richiesta

**Verifica**:
```bash
# Testa con multiple istanze
kubectl scale deployment/app --replicas=5

# Ogni richiesta può andare a pod diverso
for i in {1..10}; do
  curl http://app/greeting?name=Test$i
done
```

---

## Factor VII: Port Binding
**Principio**: *Esporta servizi via port binding*

### Implementazione nel Codice

**1. Spring Boot - Self-Contained Web Server**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- ✅ Include Tomcat embedded -->
</dependency>
```

```java
// Application.java
@SpringBootApplication
public class Application {
    
    // ✅ Embedded Tomcat si avvia automaticamente
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**2. Configurazione Porta**
```yaml
# application.yml
server:
  port: ${SERVER_PORT:8080}  # ✅ Porta configurabile
  
# ❌ SBAGLIATO: Non fare così
# Deploy su Tomcat esterno (war file)
```

**3. Dockerfile - Espone Porta**
```dockerfile
# ✅ Documenta quale porta usa l'app
EXPOSE 8080

# ✅ L'app SI AVVIA DA SOLA
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**4. Kubernetes Service - Mappa Porta**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: twelve-factor-service
spec:
  ports:
  - port: 80           # ✅ Porta esterna
    targetPort: 8080   # ✅ Porta interna del container
  selector:
    app: twelve-factor-app
```

**Confronto Architetturale**:

| ❌ Approccio Vecchio | ✅ 12-Factor (Port Binding) |
|---------------------|---------------------------|
| Deploy WAR su Tomcat esterno | JAR con Tomcat embedded |
| Configurare Tomcat a parte | App self-contained |
| Port 8080 hardcoded in Tomcat | Port configurabile via env |
| App dipende dal container | App È il container |

**Perché è importante**:
- **Portabilità**: nessuna dipendenza da web server esterno
- **Semplicità**: `java -jar app.jar` e sei online
- **Cloud-native**: perfetto per container/Kubernetes

**Verifica**:
```bash
# L'app si avvia da sola, nessun setup esterno
java -jar app.jar
# Server avviato su porta 8080

# Cambia porta via environment
SERVER_PORT=9000 java -jar app.jar
# Server ora su porta 9000
```

---

## Factor VIII: Concurrency
**Principio**: *Scala attraverso il process model*

### Implementazione nel Codice

**1. Application Stateless (prerequisito)**
```java
// ✅ Nessuno stato condiviso = scalabile
@Service
public class GreetingService {
    
    // Ogni istanza è indipendente
    private final AtomicLong counter = new AtomicLong();
    
    public Greeting createGreeting(String name) {
        // Processa richiesta senza coordinazione con altre istanze
        return new Greeting(counter.incrementAndGet(), message, version);
    }
}
```

**2. Kubernetes Horizontal Pod Autoscaler**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: twelve-factor-app
  minReplicas: 2        # ✅ Minimo 2 pod
  maxReplicas: 10       # ✅ Scala fino a 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70  # Scala al 70% CPU
```

**3. Deployment Configuration**
```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  replicas: 3  # ✅ 3 processi identici in parallelo
  template:
    spec:
      containers:
      - name: app
        image: twelve-factor-demo:1.0.0
        resources:
          requests:
            memory: "256Mi"  # ✅ Risorse per processo
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
```

**4. Service per Load Balancing**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: app-service
spec:
  type: LoadBalancer
  selector:
    app: twelve-factor-app  # ✅ Traffico distribuito tra tutti i pod
  ports:
  - port: 80
    targetPort: 8080
```

**Process Model Visualizzato**:

```
┌─────────────────────────────────────────┐
│          Load Balancer (Service)         │
└────────────┬────────────────────────────┘
             │
      ┌──────┴──────┬──────────────┐
      │             │              │
   ┌──▼──┐      ┌──▼──┐       ┌──▼──┐
   │ Pod1 │      │ Pod2 │       │ Pod3 │
   │      │      │      │       │      │
   │ JVM  │      │ JVM  │       │ JVM  │
   │:8080 │      │:8080 │       │:8080 │
   └─────┘      └─────┘       └─────┘
```

**Scaling Strategy**:

| Traffico | Pods | CPU per Pod | Totale CPU |
|----------|------|-------------|------------|
| Basso    | 2    | 10%         | 20%        |
| Medio    | 5    | 60%         | 300%       |
| Alto     | 10   | 80%         | 800%       |

**Perché è importante**:
- **Scalabilità orizzontale** > scalabilità verticale
- Gestisce 1000 req/s con 10 pod invece di 1 server gigante
- **Auto-healing**: pod muore, Kubernetes ne avvia uno nuovo

**Verifica**:
```bash
# Scala manualmente
kubectl scale deployment/app --replicas=5

# Verifica distribuzione carico
kubectl get pods
kubectl top pods  # Vedi CPU/memoria di ogni pod

# Test load balancing
for i in {1..100}; do
  curl http://app-service/greeting
done
```

---

## Factor IX: Disposability
**Principio**: *Massimizza robustezza con fast startup e graceful shutdown*

### Implementazione nel Codice

**1. Fast Startup - Minimal Initialization**
```java
@SpringBootApplication
public class Application {
    
    public static void main(String[] args) {
        // ✅ Spring Boot ottimizzato per startup veloce
        SpringApplication app = new SpringApplication(Application.class);
        
        // ✅ Disabilita lazy-initialization per startup prevedibile
        app.setLazyInitialization(false);
        
        app.run(args);
    }
}

@Service
public class GreetingService {
    
    // ✅ CORRETTO: Initialization leggera
    @PostConstruct
    public void init() {
        log.info("Service initialized");
        // NO operazioni bloccanti qui
    }
    
    // ❌ SBAGLIATO: Inizializzazione pesante
    // @PostConstruct
    // public void initializeCache() {
    //     loadMillionsOfRecords();  // Rallenta startup
    // }
}
```

**2. Graceful Shutdown Configuration**
```yaml
# application.yml
server:
  shutdown: graceful  # ✅ CHIAVE per Factor IX

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # ✅ Tempo per completare richieste
```

**3. Kubernetes Lifecycle Hooks**
```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - name: app
        image: app:1.0.0
        
        # ✅ Graceful shutdown
        lifecycle:
          preStop:
            exec:
              command: ["/bin/sh", "-c", "sleep 15"]
        
        # ✅ Termination grace period
      terminationGracePeriodSeconds: 30
```

**4. Health Checks per Fast Startup**
```yaml
# Kubernetes probes
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30    # ✅ Aspetta startup
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 20    # ✅ Prima di liveness
  periodSeconds: 5
  failureThreshold: 3
```

**5. Actuator Health Checks**
```java
// Spring Boot include health checks automatici

// ✅ Custom health check (opzionale)
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Controlla se l'app è pronta
        return Health.up()
            .withDetail("startup-time", "2s")
            .build();
    }
}
```

**6. Dockerfile - Health Check**
```dockerfile
# ✅ Health check nel container
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
```

**Shutdown Sequence Diagram**:

```
Richiesta Kill Pod
        │
        ▼
[PreStop Hook] → Sleep 15s
        │        (traffico drena)
        ▼
[App riceve SIGTERM]
        │
        ▼
[Graceful Shutdown]
        │
    ┌───┴───┐
    │ Aspetta richieste in corso
    │ (max 30s)
    └───┬───┘
        │
        ▼
[App termina]
```

**Timing Ottimale**:

| Fase | Tempo Target | Perché |
|------|-------------|--------|
| Startup | < 30s | Kubernetes readiness probe |
| Shutdown | < 30s | Termination grace period |
| PreStop | 15s | Tempo per drenare traffico |

**Perché è importante**:
- **Deployments veloci**: pod startup in secondi
- **Zero downtime**: graceful shutdown completa richieste
- **Resilienza**: riavvii frequenti non sono un problema

**Verifica**:
```bash
# Test startup time
time docker run -d app:1.0.0

# Test graceful shutdown
kubectl delete pod app-xyz-123
kubectl logs app-xyz-123  # Vedi log di shutdown pulito

# Load test durante rolling update
kubectl set image deployment/app app=app:2.0.0
# Nessuna richiesta fallita durante l'update
```

---

## Factor X: Dev/Prod Parity
**Principio**: *Mantieni development, staging e production simili*

### Implementazione nel Codice

**1. Dockerfile Identico per Tutti gli Ambienti**
```dockerfile
# ✅ STESSA immagine Docker per dev/staging/prod
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:1.18

# ✅ STESSE dipendenze
COPY --from=builder /workspace/app/target/*.jar app.jar

# ✅ STESSO entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**2. Docker Compose per Sviluppo Locale**
```yaml
# docker-compose.yml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile  # ✅ Stesso Dockerfile di prod
    ports:
      - "8080:8080"
    environment:
      # ✅ Config diversa, container identico
      - GREETING_PREFIX=Dev Environment
      - DATABASE_URL=postgres://localhost/devdb
```

**3. Kubernetes per Produzione**
```yaml
# kubernetes-deployment.yml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - name: app
        image: app:1.0.0  # ✅ Stessa immagine di dev
        env:
        - name: GREETING_PREFIX
          value: "Production"  # ✅ Solo config diversa
        - name: DATABASE_URL
          value: "postgres://prod-rds/proddb"
```

**4. Profile Spring Boot (Opzionale)**
```yaml
# application.yml
spring:
  profiles:
    active: ${SPRING_PROFILE:default}

---
# Profilo development
spring:
  config:
    activate:
      on-profile: dev
logging:
  level:
    root: DEBUG  # ✅ Più verbose in dev

---
# Profilo production
spring:
  config:
    activate:
      on-profile: prod
logging:
  level:
    root: WARN  # ✅ Meno verbose in prod
```

**5. Backing Services con Parity**
```yaml
# docker-compose.yml (Dev)
services:
  app:
    depends_on:
      - postgres
      
  postgres:
    image: postgres:15  # ✅ Stessa versione di prod
    environment:
      POSTGRES_DB: devdb
      
# Kubernetes (Prod)
# Usa RDS con PostgreSQL 15  # ✅ Stessa versione
```

**Matrice di Parity**:

| Componente | Dev | Staging | Production | Parity? |
|-----------|-----|---------|------------|---------|
| **Immagine Docker** | `app:1.0.0` | `app:1.0.0` | `app:1.0.0` | ✅ 100% |
| **Java Version** | OpenJDK 17 | OpenJDK 17 | OpenJDK 17 | ✅ 100% |
| **PostgreSQL** | 15.2 | 15.2 | 15.2 | ✅ 100% |
| **Redis** | 7.0 | 7.0 | 7.0 | ✅ 100% |
| **Config** | Env vars | Env vars | Env vars | ✅ Meccanismo |
| **Deploy** | `docker-compose` | Kubernetes | Kubernetes | ⚠️ Simile |

**Gap Comuni da Evitare**:

| ❌ Anti-Pattern | ✅ Best Practice |
|----------------|-----------------|
| Dev: SQLite, Prod: PostgreSQL | Dev e Prod: PostgreSQL (Docker) |
| Dev: Java 11, Prod: Java 17 | Stessa versione ovunque |
| Dev: file system, Prod: S3 | Dev: MinIO (S3-compatible) |
| Dev: no SSL, Prod: SSL | Dev: SSL self-signed |

**Perché è importante**:
- **"Works on my machine"** problema eliminato
- Bug riproducibili: se accade in prod, accade anche in dev
- Deploy sicuri: testato in ambiente identico

**Verifica**:
```bash
# Test che l'app funzioni identica in entrambi

# Dev
docker-compose up
curl http://localhost:8080/greeting  # Testa funzionalità

# Prod (dopo deploy)
curl https://prod.example.com/greeting  # Stessa risposta
```

---

## Factor XI: Logs
**Principio**: *Tratta i log come event streams*

### Implementazione nel Codice

**1. Logging su Stdout/Stderr**
```yaml
# application.yml
logging:
  level:
    root: INFO
  it.alf: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"  # ✅ Pattern semplice

# ❌ NO FILE LOGGING
# logging:
#   file:
#     name: app.log  # SBAGLIATO per 12-factor
```

**2. Service con Structured Logging**
```java
@Service
@Slf4j  // ✅ Lombok per logging
public class GreetingService {
    
    public Greeting createGreeting(String name) {
        long count = counter.incrementAndGet();
        
        // ✅ CORRETTO: Log su stdout via SLF4J
        log.info("Greeting created: {} (count: {})", message, count);
        
        // ✅ Log di errore su stderr
        try {
            // operazione
        } catch (Exception e) {
            log.error("Error creating greeting", e);
        }
        
        return greeting;
    }
}
```

**3. Controller con Request Logging**
```java
@RestController
@Slf4j
public class GreetingController {
    
    @GetMapping("/greeting")
    public Greeting getGreeting(@RequestParam String name) {
        
        // ✅ Log strutturato
        log.info("Request received: name={}", name);
        
        Greeting result = greetingService.createGreeting(name);
        
        log.debug("Response: {}", result);
        
        return result;
    }
}
```

**4. Dockerfile - Non Redirige Output**
```dockerfile
# ✅ CORRETTO: Output va naturalmente a stdout/stderr
ENTRYPOINT ["java", "-jar", "app.jar"]

# ❌ SBAGLIATO: Non fare redirect a file
# ENTRYPOINT ["java", "-jar", "app.jar", ">", "/var/log/app.log"]
```

**5. Kubernetes - Cattura Logs**
```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - name: app
        image: app:1.0.0
        # ✅ Kubernetes cattura stdout/stderr automaticamente
        # Non serve configurazione aggiuntiva
```

**6. Log Aggregation (Esempio con Fluentd)**
```yaml
# fluentd-config.yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: fluentd-config
data:
  fluent.conf: |
    # ✅ Cattura logs da stdout dei container
    <source>
      @type tail
      path /var/log/containers/*.log
      pos_file /var/log/fluentd-containers.log.pos
      tag kubernetes.*
      read_from_head true
      <parse>
        @type json
        time_format %Y-%m-%dT%H:%M:%S.%NZ
      </parse>
    </source>
    
    # ✅ Invia a Elasticsearch
    <match kubernetes.**>
      @type elasticsearch
      host elasticsearch
      port 9200
      logstash_format true
    </match>
```

**7. Structured Logging con JSON (Opzionale)**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

```xml
<!-- src/main/resources/logback-spring.xml -->
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- ✅ Log in formato JSON per parsing facile -->
            <includeContext>false</includeContext>
            <includeMdc>true</includeMdc>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

**Output JSON Strutturato**:
```json
{
  "timestamp": "2025-10-18T10:30:45.123Z",
  "level": "INFO",
  "logger": "service.it.alf.twelve_factor.GreetingService",
  "message": "Greeting created: Hello, World! (count: 42)",
  "thread": "http-nio-8080-exec-1",
  "context": {
    "name": "World",
    "count": 42
  }
}
```

**Log Flow Architecture**:

```
┌──────────────┐
│   App Pod    │
│              │
│  Log.info()  │──┐
└──────────────┘  │
                  │ stdout/stderr
┌──────────────┐  │
│   App Pod    │  │
│              │  │
│  Log.error() │──┤
└──────────────┘  │
                  ▼
         ┌────────────────┐
         │   Kubernetes   │
         │   Log Driver   │
         └────────┬───────┘
                  │
                  ▼
         ┌────────────────┐
         │    Fluentd     │
         │  (Aggregator)  │
         └────────┬───────┘
                  │
         ┌────────┴────────┬──────────┐
         ▼                 ▼          ▼
    ┌─────────┐      ┌─────────┐  ┌──────────┐
    │Elastic- │      │CloudWatch│  │  Splunk  │
    │ search  │      │   Logs   │  │          │
    └─────────┘      └─────────┘  └──────────┘
```

**Vantaggi vs File Logging**:

| File Logging (❌) | Stdout/Stderr (✅) |
|------------------|-------------------|
| Va su disco locale | Va a log aggregator |
| Disco pieno = app crash | Infinito (streaming) |
| Serve SSH per vedere log | `kubectl logs` / Dashboard |
| Log persi se pod muore | Log persistiti centralmente |
| Rotation manuale | Gestito automaticamente |

**Perché è importante**:
- **Debugging distribuito**: vedi log di 10 pod insieme
- **Analisi centralizzata**: query su milioni di log
- **Retention**: log conservati anche se pod viene distrutto

**Verifica**:
```bash
# Vedi log in tempo reale
kubectl logs -f deployment/app

# Vedi log di tutti i pod
kubectl logs -l app=twelve-factor-app --tail=100

# Log aggregation query (esempio Elasticsearch)
GET /logs/_search
{
  "query": {
    "match": {
      "message": "error"
    }
  }
}
```

---

## Factor XII: Admin Processes
**Principio**: *Esegui task amministrativi come processi one-off*

### Implementazione nel Codice

**1. Spring Boot Actuator per Admin**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**2. Actuator Endpoints Configuration**
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env,configprops
  endpoint:
    health:
      show-details: always  # ✅ Mostra dettagli health
  metrics:
    export:
      prometheus:
        enabled: true  # ✅ Esporta metriche
```

**3. Admin Controller**
```java
@RestController
@RequestMapping("/admin")
public class AdminController {
    
    @Autowired
    private Environment environment;
    
    // ✅ Endpoint per diagnostica
    @GetMapping("/config")
    public Map<String, String> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("greeting.prefix", environment.getProperty("app.greeting.prefix"));
        config.put("version", environment.getProperty("app.version"));
        return config;
    }
    
    // ✅ Endpoint per metriche custom
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        // Statistiche applicazione
        return stats;
    }
}
```

**4. Database Migrations come Admin Process**
```java
// ✅ Flyway per migrations
@Configuration
public class FlywayConfig {
    
    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load();
        
        // ✅ Migration eseguita all'avvio
        flyway.migrate();
        
        return flyway;
    }
}
```

**5. Kubernetes Job per Admin Tasks**
```yaml
# kubernetes-migration-job.yml
apiVersion: batch/v1
kind: Job
metadata:
  name: database-migration
spec:
  template:
    spec:
      containers:
      - name: migration
        image: app:1.0.0
        # ✅ Stessa immagine, comando diverso
        command: ["java"]
        args: ["-cp", "app.jar", "org.springframework.boot.loader.JarLauncher", "--spring.flyway.enabled=true"]
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
      restartPolicy: OnFailure
```

**6. kubectl exec per Task Interattivi**
```bash
# ✅ Esegui comando one-off nel container
kubectl exec -it deployment/app -- java -jar app.jar --command=clearCache

# ✅ Console interattiva
kubectl exec -it deployment/app -- /bin/bash
```

**7. Custom Admin Commands**
```java
@Component
public class AdminCommands implements CommandLineRunner {
    
    @Value("${admin.command:none}")
    private String command;
    
    @Override
    public void run(String... args) {
        if ("clearCache".equals(command)) {
            // ✅ Task amministrativo
            clearCacheOperation();
            System.exit(0);
        }
    }
}
```

**8. Prometheus Metrics**
```java
@Service
public class GreetingService {
    
    private final Counter greetingCounter;
    
    public GreetingService(MeterRegistry meterRegistry) {
        // ✅ Custom metrics
        this.greetingCounter = Counter.builder("greeting.created")
            .description("Number of greetings created")
            .register(meterRegistry);
    }
    
    public Greeting createGreeting(String name) {
        greetingCounter.increment();
        // ...
    }
}
```

**Admin Endpoints Disponibili**:

| Endpoint | Scopo | Esempio Output |
|----------|-------|----------------|
| `/actuator/health` | Stato applicazione | `{"status": "UP"}` |
| `/actuator/metrics` | Lista metriche | `["jvm.memory.used", "http.requests"]` |
| `/actuator/prometheus` | Metriche Prometheus | `greeting_created_total 142` |
| `/actuator/env` | Environment variables | `{"SERVER_PORT": "8080"}` |
| `/actuator/info` | Info applicazione | `{"version": "1.0.0"}` |

**Admin Process Patterns**:

```yaml
# Pattern 1: Job one-off
apiVersion: batch/v1
kind: Job
metadata:
  name: data-import
spec:
  template:
    spec:
      containers:
      - name: import
        image: app:1.0.0
        command: ["java", "-jar", "app.jar", "--import=/data/file.csv"]
      restartPolicy: Never

---
# Pattern 2: CronJob periodico
apiVersion: batch/v1
kind: CronJob
metadata:
  name: daily-report
spec:
  schedule: "0 2 * * *"  # ✅ Ogni giorno alle 2 AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: report
            image: app:1.0.0
            command: ["java", "-jar", "app.jar", "--generate-report"]
          restartPolicy: OnFailure
```

**Monitoring Dashboard (Grafana Query)**:
```promql
# ✅ Query Prometheus per metriche
rate(http_requests_total[5m])
greeting_created_total
jvm_memory_used_bytes{area="heap"}
```

**9. Init Containers per Setup Tasks**
```yaml
# kubernetes-deployment.yml con initContainer
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      # ✅ InitContainer per task pre-startup
      initContainers:
      - name: migration
        image: app:1.0.0
        command: ["java"]
        args: [
          "-Dspring.flyway.enabled=true",
          "-Dspring.main.web-application-type=none",
          "-jar", "app.jar"
        ]
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
      
      # Container principale si avvia DOPO migration
      containers:
      - name: app
        image: app:1.0.0
```

**10. Spring Boot Admin Server (Opzionale)**
```java
// AdminServerApplication.java
@SpringBootApplication
@EnableAdminServer  // ✅ Dashboard per monitoring
public class AdminServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminServerApplication.class, args);
    }
}
```

```xml
<!-- pom.xml per Admin Server -->
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>spring-boot-admin-starter-server</artifactId>
    <version>3.1.8</version>
</dependency>
```

```yaml
# Client configuration
spring:
  boot:
    admin:
      client:
        url: http://admin-server:8080  # ✅ Registra con Admin Server
```

**11. Management Endpoints Security**
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized  # ✅ Sicurezza
  
spring:
  security:
    user:
      name: ${ADMIN_USER:admin}
      password: ${ADMIN_PASSWORD:changeme}
```

```java
// SecurityConfig.java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // ✅ Endpoint pubblici
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // ✅ Admin endpoints protetti
                .requestMatchers("/actuator/**").authenticated()
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults());
        
        return http.build();
    }
}
```

**12. Custom Admin CLI Tool**
```java
// AdminCLI.java
@Component
public class AdminCLI implements CommandLineRunner {
    
    @Autowired
    private GreetingService greetingService;
    
    @Value("${admin.task:none}")
    private String task;
    
    @Override
    public void run(String... args) throws Exception {
        switch (task) {
            case "reset-counter":
                resetCounter();
                break;
            case "export-data":
                exportData();
                break;
            case "import-data":
                importData(args);
                break;
            default:
                // ✅ Nessun task admin, avvia normalmente
                return;
        }
        
        // ✅ Task completato, termina
        System.exit(0);
    }
    
    private void resetCounter() {
        log.info("Resetting counter...");
        greetingService.resetCounter();
        log.info("Counter reset completed");
    }
    
    private void exportData() {
        log.info("Exporting data...");
        // Export logic
        log.info("Export completed");
    }
    
    private void importData(String[] args) {
        String file = args.length > 0 ? args[0] : "/data/import.csv";
        log.info("Importing data from: {}", file);
        // Import logic
        log.info("Import completed");
    }
}
```

**Uso del CLI**:
```bash
# ✅ Esegui task admin con stessa immagine
kubectl run admin-task --rm -it \
  --image=app:1.0.0 \
  --restart=Never \
  --env="admin.task=reset-counter" \
  -- java -jar app.jar

# ✅ Import dati
kubectl run data-import --rm -it \
  --image=app:1.0.0 \
  --restart=Never \
  --env="admin.task=import-data" \
  -- java -jar app.jar /mnt/data/file.csv
```

**13. Database Console Access**
```yaml
# kubernetes-job-db-console.yml
apiVersion: batch/v1
kind: Job
metadata:
  name: db-console
spec:
  template:
    spec:
      containers:
      - name: psql
        image: postgres:15
        command: ["psql"]
        args: ["$(DATABASE_URL)"]
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
        stdin: true
        tty: true
      restartPolicy: Never
```

**14. Monitoring e Alerting Integration**
```yaml
# prometheus-rules.yml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: app-alerts
spec:
  groups:
  - name: app
    interval: 30s
    rules:
    # ✅ Alert se troppi errori
    - alert: HighErrorRate
      expr: rate(http_requests_total{status="500"}[5m]) > 0.05
      annotations:
        summary: "High error rate detected"
    
    # ✅ Alert se app down
    - alert: ApplicationDown
      expr: up{job="twelve-factor-app"} == 0
      for: 2m
      annotations:
        summary: "Application is down"
```

**Admin Tasks Architecture**:

```
┌─────────────────────────────────────────────────┐
│              Admin Processes Layer               │
├─────────────────────────────────────────────────┤
│                                                   │
│  ┌──────────────┐  ┌──────────────┐            │
│  │   Actuator   │  │  Prometheus  │  ← Monitoring│
│  │   Endpoints  │  │   Metrics    │            │
│  └──────────────┘  └──────────────┘            │
│                                                   │
│  ┌──────────────┐  ┌──────────────┐            │
│  │ Kubernetes   │  │   CronJobs   │  ← Scheduled│
│  │    Jobs      │  │              │    Tasks   │
│  └──────────────┘  └──────────────┘            │
│                                                   │
│  ┌──────────────┐  ┌──────────────┐            │
│  │  kubectl     │  │  Admin CLI   │  ← One-off │
│  │    exec      │  │    Tool      │    Tasks   │
│  └──────────────┘  └──────────────┘            │
│                                                   │
└─────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────┐
│           Application Container                  │
│         (Stessa immagine Docker)                │
└─────────────────────────────────────────────────┘
```

**Confronto Approcci Admin**:

| Task | ❌ Anti-Pattern | ✅ 12-Factor Way |
|------|----------------|-----------------|
| **DB Migration** | SSH + SQL script | Kubernetes Job con Flyway |
| **Monitoring** | SSH + tail logs | Actuator + Prometheus |
| **Data Export** | Cron su server | CronJob Kubernetes |
| **Cache Clear** | Redis CLI diretto | Admin endpoint HTTP |
| **Health Check** | Ping manuale | Kubernetes probes |
| **Metrics** | Log parsing | Prometheus scraping |

**Perché è importante**:
- **Stesso ambiente**: admin tasks usano stessa immagine dell'app
- **Nessun accesso diretto**: tutto passa attraverso l'applicazione
- **Automazione**: CronJobs per task ricorrenti
- **Osservabilità**: metriche, health checks, tracing integrati
- **Sicurezza**: nessun accesso SSH ai container

**Verifica Completa Factor XII**:
```bash
# 1. Health checks
curl http://app:8080/actuator/health
# Risposta: {"status":"UP","components":{...}}

# 2. Metriche disponibili
curl http://app:8080/actuator/metrics
# Lista tutte le metriche disponibili

# 3. Metrica specifica
curl http://app:8080/actuator/metrics/greeting.created
# {"name":"greeting.created","measurements":[{"value":142.0}]}

# 4. Prometheus format
curl http://app:8080/actuator/prometheus | grep greeting
# greeting_created_total 142.0

# 5. Info applicazione
curl http://app:8080/actuator/info
# {"app":{"version":"1.0.0"},...}

# 6. Environment variables (protetto)
curl -u admin:password http://app:8080/actuator/env

# 7. Esegui database migration
kubectl apply -f migration-job.yml
kubectl wait --for=condition=complete job/database-migration
kubectl logs job/database-migration

# 8. Task one-off
kubectl run admin-reset --rm -it \
  --image=app:1.0.0 \
  --restart=Never \
  --env="admin.task=reset-counter" \
  -- java -jar app.jar

# 9. CronJob status
kubectl get cronjobs
kubectl get jobs --selector=cronjob=daily-report

# 10. Logs aggregati
kubectl logs -l app=twelve-factor-app --tail=100

# 11. Exec interattivo (emergency)
kubectl exec -it deployment/app -- /bin/bash

# 12. Port-forward per debugging
kubectl port-forward deployment/app 8080:8080
# Ora accedi a http://localhost:8080/actuator

# 13. Metriche Prometheus (se Prometheus installato)
kubectl port-forward svc/prometheus 9090:9090
# Query: rate(greeting_created_total[5m])

# 14. Grafana dashboard (se Grafana installato)
kubectl port-forward svc/grafana 3000:3000
# Visualizza dashboard con metriche app
```

**Best Practices Factor XII**:

1. **✅ Sempre usare stessa immagine**: admin tasks e app condividono codebase
2. **✅ Mai SSH nei container**: usa `kubectl exec` solo per debugging
3. **✅ Automazione**: CronJobs invece di cron manuale
4. **✅ Observability**: Actuator + Prometheus + Grafana
5. **✅ Security**: protezione endpoint admin
6. **✅ Documentation**: documenta tutti gli admin endpoints
7. **✅ Idempotenza**: admin tasks devono essere rilanciabili
8. **✅ Logging**: ogni admin task deve loggare azioni
9. **✅ RBAC**: permessi Kubernetes per admin tasks
10. **✅ Audit trail**: traccia chi esegue cosa

**Esempio Completo: Backup Database Schedulato**

```yaml
# cronjob-backup.yml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: database-backup
spec:
  schedule: "0 2 * * *"  # ✅ Ogni notte alle 2 AM
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 1
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: backup
            image: app:1.0.0
            command: ["java"]
            args: [
              "-Dadmin.task=backup-database",
              "-jar", "app.jar"
            ]
            env:
            - name: DATABASE_URL
              valueFrom:
                secretKeyRef:
                  name: db-credentials
                  key: url
            - name: BACKUP_BUCKET
              value: "s3://backups/postgres"
            volumeMounts:
            - name: backup-storage
              mountPath: /backups
          volumes:
          - name: backup-storage
            persistentVolumeClaim:
              claimName: backup-pvc
          restartPolicy: OnFailure
```

```java
// BackupService.java
@Service
public class BackupService {
    
    @Value("${backup.bucket}")
    private String backupBucket;
    
    public void performBackup() {
        log.info("Starting database backup to {}", backupBucket);
        
        try {
            // ✅ Esegui pg_dump
            String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            );
            String filename = String.format("backup_%s.sql", timestamp);
            
            ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-h", dbHost,
                "-U", dbUser,
                "-d", dbName,
                "-f", "/backups/" + filename
            );
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // ✅ Upload a S3
                uploadToS3(filename);
                log.info("Backup completed successfully: {}", filename);
            } else {
                log.error("Backup failed with exit code: {}", exitCode);
                throw new BackupException("Backup failed");
            }
            
        } catch (Exception e) {
            log.error("Backup error", e);
            // ✅ Invia alert
            sendAlert("Backup Failed", e.getMessage());
            throw new RuntimeException("Backup failed", e);
        }
    }
}
```

**Monitoring Dashboard Queries (PromQL)**:

```promql
# Request rate
rate(http_requests_total[5m])

# Error rate
rate(http_requests_total{status=~"5.."}[5m])

# Response time (95th percentile)
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# Active greetings
greeting_created_total

# JVM Memory
jvm_memory_used_bytes{area="heap"}

# Garbage Collection
rate(jvm_gc_pause_seconds_count[5m])

# Admin tasks executed
admin_tasks_total{task="backup"}
```

---

## 📋 Checklist Completa di Verifica

### Factor I: Codebase
- [ ] Singola repository Git
- [ ] `.gitignore` esclude artefatti di build
- [ ] Nessun file binario committato
- [ ] Tag/branch per release

### Factor II: Dependencies
- [ ] `pom.xml` con versioni esplicite
- [ ] Nessuna dipendenza dal sistema operativo
- [ ] Dockerfile multi-stage isola build
- [ ] `mvn clean package` funziona ovunque

### Factor III: Config
- [ ] Tutte le config via environment variables
- [ ] Nessun secret nel codice
- [ ] `application.yml` usa `${VAR:default}`
- [ ] ConfigMap/Secrets in Kubernetes

### Factor IV: Backing Services
- [ ] Database/cache URL configurabili
- [ ] Connessioni via pool
- [ ] Facile sostituzione servizi
- [ ] Nessun hardcoding di endpoint

### Factor V: Build, Release, Run
- [ ] Build produce JAR immutabile
- [ ] Docker image taggata con versione
- [ ] CI/CD separa build/release/deploy
- [ ] Rollback possibile via kubectl

### Factor VI: Processes
- [ ] Applicazione stateless
- [ ] Nessuna sessione in memoria
- [ ] Stato persistente in backing services
- [ ] Multiple replicas funzionano

### Factor VII: Port Binding
- [ ] Tomcat embedded (no WAR)
- [ ] Porta configurabile via `SERVER_PORT`
- [ ] `EXPOSE` in Dockerfile
- [ ] `java -jar app.jar` avvia tutto

### Factor VIII: Concurrency
- [ ] HorizontalPodAutoscaler configurato
- [ ] Limiti CPU/memoria definiti
- [ ] Load balancing funzionante
- [ ] Scaling da 2 a 10 pod

### Factor IX: Disposability
- [ ] Startup < 30 secondi
- [ ] `shutdown: graceful` in config
- [ ] PreStop hook in Kubernetes
- [ ] Health checks (liveness/readiness)

### Factor X: Dev/Prod Parity
- [ ] Stessa immagine Docker
- [ ] Stesse versioni dipendenze
- [ ] Docker Compose per dev locale
- [ ] Backing services stesse versioni

### Factor XI: Logs
- [ ] Log su stdout/stderr
- [ ] Nessun file di log locale
- [ ] `kubectl logs` funziona
- [ ] Structured logging (opzionale JSON)

### Factor XII: Admin Processes
- [ ] Actuator endpoints esposti
- [ ] Prometheus metrics
- [ ] Jobs Kubernetes per migrations
- [ ] Health checks dettagliati

---

## 🚀 Quick Start per Sviluppatore

```bash
# 1. Clone repository
git clone https://github.com/your-repo/twelve-factor-demo.git
cd twelve-factor-demo

# 2. Build
mvn clean package

# 3. Run locale
java -jar target/twelve-factor-demo-1.0.0.jar

# 4. Build Docker
docker build -t twelve-factor-demo:1.0.0 .

# 5. Run con Docker
docker run -p 8080:8080 \
  -e GREETING_PREFIX="Dev" \
  -e APP_VERSION="1.0.0" \
  twelve-factor-demo:1.0.0

# 6. Test
curl http://localhost:8080/api/v1/greeting?name=Developer
curl http://localhost:8080/actuator/health

# 7. Deploy Kubernetes (prod)
kubectl apply -f kubernetes-configmap.yml
kubectl apply -f kubernetes-deployment.yml
kubectl apply -f kubernetes-service.yml

# 8. Verifica
kubectl get pods
kubectl logs -f deployment/twelve-factor-app
```

---

## 📚 Risorse Aggiuntive

- [12-Factor App Original](https://12factor.net/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Red Hat UBI Images](https://catalog.redhat.com/software/containers/search)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)

---

## 🎓 Conclusioni

Ogni factor è stato implementato con interventi specifici nel codice:

1. **Configuration as Code**: tutto è dichiarativo
2. **Separation of Concerns**: build ≠ config ≠ runtime
3. **Cloud-Native**: pronto per Kubernetes/OpenShift
4. **Production-Ready**: observability, scalability, resilience

L'applicazione è ora **veramente cloud-native** e segue le best practices moderne per microservizi containerizzati!