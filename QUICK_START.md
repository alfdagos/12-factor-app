# 🚀 Quick Start Guide

## 📖 Introduzione

Questo progetto implementa i principi **[12-Factor App](https://12factor.net/)** per costruire applicazioni **cloud-native** scalabili e production-ready.

Ogni componente del codice include commenti che identificano i principi implementati e le best practices applicate.

## 🚀 Primi Passi

### 1. Esplora il Codice (5 minuti)

Apri questi file in ordine per comprendere l'architettura:

```
1. src/main/java/it/alf/twelve_factor/Application.java
   👉 Entry point dell'applicazione cloud-native (Factor I, VII, IX)

2. src/main/java/it/alf/twelve_factor/service/GreetingService.java
   👉 Business logic stateless e configurabile (Factor III, VI, XI)

3. src/main/resources/application.yml
   👉 Configurazione esternalizzata (Factor III)

4. Dockerfile
   👉 Containerizzazione multi-stage (Factor II, V, X)

5. k8s/deployment.yaml
   👉 Deployment Kubernetes (Factor VIII, IX)
```

### 2. Esecuzione Locale (10 minuti)

```bash
# Compila il progetto
mvn clean package

# Esegui l'app
java -jar target/twelve-factor-demo-1.0.0.jar

# Prova l'API
curl http://localhost:8080/api/v1/greeting?name=Developer
# Risposta: {"id":1,"message":"Hello, Developer!","version":"1.0.0"}

# Modifica configurazione con env var (Factor III)
export GREETING_PREFIX="Ciao"
java -jar target/twelve-factor-demo-1.0.0.jar

# Verifica il cambiamento
curl http://localhost:8080/api/v1/greeting?name=Developer
# Risposta: {"id":1,"message":"Ciao, Developer!","version":"1.0.0"}
```

### 3. Containerizzazione con Docker (15 minuti)

```bash
# Build immagine Docker (Factor V)
docker build -t twelve-factor-demo:1.0.0 .

# Run con configurazione diversa (Factor III + X)
docker run -p 8080:8080 \
  -e GREETING_PREFIX="Hola" \
  -e APP_VERSION="1.0.0-docker" \
  twelve-factor-demo:1.0.0

# Testa
curl http://localhost:8080/api/v1/greeting?name=Docker
# Risposta: {"id":1,"message":"Hola, Docker!","version":"1.0.0-docker"}
```

### 4. Deploy su Kubernetes (20 minuti)

```bash
# Applica tutti i manifesti
kubectl apply -f k8s/

# Verifica deployment
kubectl get pods
kubectl get services

# Port forward
kubectl port-forward service/twelve-factor-demo-service 8080:8080

# Testa
curl http://localhost:8080/api/v1/greeting?name=Kubernetes

# Scala l'app (Factor VIII)
kubectl scale deployment/twelve-factor-demo --replicas=5
kubectl get pods

# Vedi i logs (Factor XI)
kubectl logs -f deployment/twelve-factor-demo
```

## 📚 Dove Trovare Ogni Fattore

### Factor I: Codebase
- 📁 **Dove**: Tutto il repository Git
- 💡 **Concetto**: Un repository, molti deploy
- 🔍 **Guarda**: `.gitignore`, `README.md`

### Factor II: Dependencies
- 📁 **Dove**: `pom.xml`, `Dockerfile`
- 💡 **Concetto**: Dipendenze esplicite e isolate
- 🔍 **Guarda**: Commenti in `pom.xml` e `Dockerfile` (stage BUILD)

### Factor III: Config
- 📁 **Dove**: `application.yml`, `k8s/configmap.yaml`, `GreetingService.java`
- 💡 **Concetto**: Config esternalizzata tramite env vars
- 🔍 **Guarda**: Sintassi `${VAR:default}` e annotation `@Value`
- 🧪 **Prova**: Cambia `GREETING_PREFIX` e riavvia

### Factor IV: Backing Services
- 📁 **Dove**: `application.yml` (esempi commentati), `docker-compose.yml`
- 💡 **Concetto**: DB/cache come risorse collegate
- 🔍 **Guarda**: Commenti su DATABASE_URL, REDIS_HOST

### Factor V: Build, Release, Run
- 📁 **Dove**: `Dockerfile` (multi-stage), processo Maven
- 💡 **Concetto**: Build → JAR, Release → immagine, Run → container
- 🔍 **Guarda**: Dockerfile stage BUILD e RUNTIME separati

### Factor VI: Processes
- 📁 **Dove**: `GreetingService.java`, `GreetingController.java`
- 💡 **Concetto**: App stateless, scalabile
- 🔍 **Guarda**: Commenti "stateless", nessuna sessione in memoria
- 🧪 **Prova**: Test `GreetingServiceTest.java`

### Factor VII: Port Binding
- 📁 **Dove**: `Application.java`, `application.yml`, `pom.xml` (Tomcat embedded)
- 💡 **Concetto**: Self-contained, web server incluso
- 🔍 **Guarda**: `server.port: ${SERVER_PORT:8080}`

### Factor VIII: Concurrency
- 📁 **Dove**: `k8s/deployment.yaml`
- 💡 **Concetto**: Scala orizzontalmente con multiple istanze
- 🔍 **Guarda**: `replicas: 2`
- 🧪 **Prova**: `kubectl scale --replicas=5`

### Factor IX: Disposability
- 📁 **Dove**: `application.yml`, `k8s/deployment.yaml`, `Dockerfile`
- 💡 **Concetto**: Startup veloce, shutdown pulito
- 🔍 **Guarda**: `shutdown: graceful`, `readinessProbe`, `livenessProbe`

### Factor X: Dev/Prod Parity
- 📁 **Dove**: `Dockerfile`, `docker-compose.yml`
- 💡 **Concetto**: Stessa immagine, config diversa
- 🔍 **Guarda**: Stesso Dockerfile per dev e prod

### Factor XI: Logs
- 📁 **Dove**: `application.yml`, `GreetingService.java`
- 💡 **Concetto**: Log su stdout, no file
- 🔍 **Guarda**: `log.info()` in GreetingService
- 🧪 **Prova**: `kubectl logs deployment/twelve-factor-demo`

### Factor XII: Admin Processes
- 📁 **Dove**: `application.yml` (Actuator), `pom.xml`
- 💡 **Concetto**: Admin tasks come processi one-off
- 🔍 **Guarda**: `management.endpoints`
- 🧪 **Prova**: `curl http://localhost:8080/actuator/health`

## 🧪 Esercizi Pratici

### Esercizio 1: Cambia la Configurazione (Factor III)
```bash
# Modifica GREETING_PREFIX via env var
export GREETING_PREFIX="Buongiorno"
mvn spring-boot:run

# Testa: http://localhost:8080/api/v1/greeting?name=Studente
# Aspettato: "Buongiorno, Studente!"
```

### Esercizio 2: Scala l'Applicazione (Factor VIII)
```bash
# Deploy su Kubernetes
kubectl apply -f k8s/

# Scala a 10 istanze
kubectl scale deployment/twelve-factor-demo --replicas=10

# Verifica
kubectl get pods

# Load test
for i in {1..1000}; do
  curl http://localhost:8080/api/v1/greeting?name=Test$i
done
```

### Esercizio 3: Simula Crash e Verifica Auto-Healing (Factor IX)
```bash
# Elimina un pod
kubectl delete pod <pod-name>

# Kubernetes lo ricrea automaticamente!
kubectl get pods -w
```

### Esercizio 4: Aggiungi una Metrica Custom (Factor XII)
```java
// In GreetingService.java
@Service
public class GreetingService {
    private final Counter greetingCounter;
    
    public GreetingService(MeterRegistry meterRegistry) {
        this.greetingCounter = Counter.builder("greeting.created")
            .description("Number of greetings created")
            .register(meterRegistry);
    }
    
    public Greeting createGreeting(String name) {
        greetingCounter.increment();
        // ... resto del codice
    }
}
```

Poi verifica:
```bash
curl http://localhost:8080/actuator/prometheus | grep greeting_created
```

## 📖 Lettura Consigliata

1. **Inizia qui**: README.md principale
2. **Deployment Kubernetes**: k8s/README.md
3. **Codice commentato**: Leggi tutti i file Java
4. **Test**: `src/test/` per vedere esempi pratici

## 🎯 Obiettivi di Apprendimento

Completando questo progetto, sarai in grado di:

- ✅ Comprendere e applicare i principi 12-factor
- ✅ Implementare configurazione esternalizzata
- ✅ Progettare applicazioni stateless e scalabili
- ✅ Containerizzare applicazioni Spring Boot
- ✅ Deployare su Kubernetes con best practices
- ✅ Implementare health checks e monitoring
- ✅ Gestire logs in modo cloud-native
- ✅ Scalare applicazioni orizzontalmente

## ❓ FAQ

**Q: Perché usare `${VAR:default}` in application.yml?**  
A: Permette configurazione diversa per ambiente senza modificare il codice (Factor III).

**Q: Perché il service è stateless?**  
A: Consente scalabilità orizzontale con multiple istanze (Factor VI, VIII).

**Q: Perché multi-stage Dockerfile?**  
A: Separa build da runtime e riduce la dimensione dell'immagine (Factor V).

**Q: Perché log su stdout?**  
A: Permette a Kubernetes/Docker di raccogliere i log centralmente (Factor XI).

## 🤝 Contributi

Hai trovato un problema o vuoi migliorare il progetto?

1. Fai un fork del repository
2. Crea un branch per le tue modifiche
3. Invia una pull request

## 📞 Supporto

Per domande o problemi, apri una issue su GitHub specificando:
- Obiettivo da raggiungere
- Passi già effettuati
- Eventuali errori riscontrati

---

**🚀 Pronto per iniziare!**

I 12-Factor App rappresentano le fondamenta per costruire applicazioni moderne, scalabili e cloud-native.
