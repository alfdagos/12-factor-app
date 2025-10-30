# Kubernetes Deployment - 12-Factor App# 🐳 Kubernetes Configuration per Docker Desktop



Questa directory contiene i manifesti Kubernetes che implementano diversi principi dei 12-Factor App.## 🧭 Struttura del progetto Kubernetes



## 📁 File e Fattori Implementati```

k8s/

| File | Fattore | Scopo |├── configmap.yaml

|------|---------|-------|├── secret.yaml

| `configmap.yaml` | **III: Config** | Configurazione esternalizzata |├── deployment.yaml

| `secret.yaml` | **III: Config** | Secrets sicuri (esempio) |├── service.yaml

| `deployment.yaml` | **VI, VIII, IX** | Deployment stateless, scalabile, con health checks |└── ingress.yaml   # opzionale

| `service.yaml` | **VII: Port Binding** | Espone l'app tramite service |```

| `ingress.yaml` | **VII: Port Binding** | Accesso HTTP esterno |

---

---

## ⚙️ 1. ConfigMap

## 🚀 Quick Start

**k8s/configmap.yaml**

### 1. Build dell'immagine Docker

```yaml

```bashapiVersion: v1

# Build dell'immagine (Factor V: Build)kind: ConfigMap

docker build -t twelve-factor-demo:1.0.0 .metadata:

  name: twelve-factor-demo-config

# Verifica che l'immagine sia disponibiledata:

docker images | grep twelve-factor-demo  SPRING_PROFILES_ACTIVE: "prod"

```  APP_MESSAGE: "Hello from Kubernetes! This is the 12-Factor App."

```

### 2. Deploy su Kubernetes

---

```bash

# Applica tutti i manifesti## 🔐 2. Secret

kubectl apply -f k8s/

**k8s/secret.yaml**

# Verifica il deployment

kubectl get pods```yaml

kubectl get servicesapiVersion: v1

kubectl get ingresskind: Secret

```metadata:

  name: twelve-factor-demo-secret

### 3. Test dell'applicazionetype: Opaque

data:

```bash  DB_PASSWORD: bXlwYXNzd29yZA==   # base64("mypassword")

# Port forward per test locale```

kubectl port-forward service/twelve-factor-demo-service 8080:8080

---

# Test API

curl http://localhost:8080/api/v1/greeting?name=Kubernetes## 🚀 3. Deployment



# Test health check (Factor XII)**k8s/deployment.yaml**

curl http://localhost:8080/actuator/health

```yaml

# Test metriche Prometheus (Factor XII)apiVersion: apps/v1

curl http://localhost:8080/actuator/prometheuskind: Deployment

```metadata:

  name: twelve-factor-demo

---  labels:

    app: twelve-factor-demo

## 📖 Spiegazione dei Manifestispec:

  replicas: 2

### ConfigMap - Factor III: Config  selector:

    matchLabels:

Il `configmap.yaml` esternalizza la configurazione:      app: twelve-factor-demo

  template:

```yaml    metadata:

apiVersion: v1      labels:

kind: ConfigMap        app: twelve-factor-demo

metadata:    spec:

  name: twelve-factor-demo-config      # Optional: set imagePullSecrets if your image is hosted on a private registry

data:      # imagePullSecrets:

  SPRING_PROFILES_ACTIVE: "prod"      # - name: my-registry-secret

  SERVER_PORT: "8080"      terminationGracePeriodSeconds: 30

  GREETING_PREFIX: "Hello from Kubernetes"      securityContext:

  APP_VERSION: "1.0.0"        runAsNonRoot: true

```        runAsUser: 1000

      containers:

**Perché è importante:**        - name: twelve-factor-demo

- ✅ Stessa immagine Docker per tutti gli ambienti          # Use a local image name by default so it works in local clusters (minikube/kind)

- ✅ Cambio configurazione senza rebuild          # Replace with your registry image (e.g. myorg/twelve-factor-demo:1.0.0) for production

- ✅ Config diversa per dev/staging/prod          image: twelve-factor-demo:1.0.0

          imagePullPolicy: IfNotPresent

**Test del Factor III:**          ports:

```bash            - containerPort: 8080

# Modifica il greeting prefix          envFrom:

kubectl edit configmap twelve-factor-demo-config            - configMapRef:

# Cambia GREETING_PREFIX: "Ciao from Kubernetes"                name: twelve-factor-demo-config

            - secretRef:

# Riavvia i pod per applicare la nuova config                name: twelve-factor-demo-secret

kubectl rollout restart deployment/twelve-factor-demo          env:

            - name: SERVER_PORT

# Verifica il cambiamento              valueFrom:

curl http://localhost:8080/api/v1/greeting?name=World                configMapKeyRef:

# Risposta: {"id":1,"message":"Ciao from Kubernetes, World!","version":"1.0.0"}                  name: twelve-factor-demo-config

```                  key: SERVER_PORT

            - name: GREETING_PREFIX

### Deployment - Factor VI, VIII, IX              valueFrom:

                configMapKeyRef:

Il `deployment.yaml` implementa:                  name: twelve-factor-demo-config

                  key: GREETING_PREFIX

**Factor VI: Processes (Stateless)**            - name: APP_VERSION

```yaml              valueFrom:

spec:                configMapKeyRef:

  replicas: 2  # ✅ Multiple istanze possibili solo se stateless                  name: twelve-factor-demo-config

```                  key: APP_VERSION

            - name: APP_MESSAGE

**Factor VIII: Concurrency (Scalabilità)**              valueFrom:

```yaml                configMapKeyRef:

resources:                  name: twelve-factor-demo-config

  requests:                  key: APP_MESSAGE

    cpu: "100m"          resources:

    memory: "128Mi"            requests:

  limits:              cpu: "100m"

    cpu: "500m"              memory: "128Mi"

    memory: "512Mi"            limits:

```              cpu: "500m"

              memory: "512Mi"

**Factor IX: Disposability (Fast startup, Graceful shutdown)**          readinessProbe:

```yaml            httpGet:

terminationGracePeriodSeconds: 30  # ✅ Tempo per shutdown pulito              path: /actuator/health

              port: 8080

readinessProbe:  # ✅ Indica quando il pod è pronto            initialDelaySeconds: 10

  httpGet:            periodSeconds: 10

    path: /actuator/health/readiness            failureThreshold: 3

    port: 8080          livenessProbe:

  initialDelaySeconds: 10            httpGet:

              path: /actuator/health

livenessProbe:  # ✅ Indica se il pod è vivo              port: 8080

  httpGet:            initialDelaySeconds: 30

    path: /actuator/health/liveness            periodSeconds: 20

    port: 8080            failureThreshold: 3

  initialDelaySeconds: 30```

```

---

**Test dei Factor VI, VIII, IX:**

```bash## 🌐 4. Service

# Factor VIII: Scala a 5 istanze

kubectl scale deployment/twelve-factor-demo --replicas=5**k8s/service.yaml**

kubectl get pods

```yaml

# Factor IX: Verifica graceful shutdownapiVersion: v1

kubectl delete pod <pod-name>kind: Service

kubectl logs <pod-name>  # Vedi log di shutdown pulitometadata:

  name: twelve-factor-demo-service

# Factor VI: Load test su multiple istanzespec:

for i in {1..100}; do  selector:

  curl http://localhost:8080/api/v1/greeting?name=Test$i    app: twelve-factor-demo

done  ports:

```  - protocol: TCP

    port: 80

### Service - Factor VII: Port Binding    targetPort: 8080

  type: ClusterIP  # Modificato da NodePort per Docker Desktop

Il `service.yaml` espone l'applicazione:```



```yaml---

apiVersion: v1

kind: Service## 🌍 5. Ingress (raccomandato per Docker Desktop)

metadata:

  name: twelve-factor-demo-service**k8s/ingress.yaml**

spec:

  selector:```yaml

    app: twelve-factor-demoapiVersion: networking.k8s.io/v1

  ports:kind: Ingress

    - port: 8080        # Porta del servicemetadata:

      targetPort: 8080  # Porta del container (configurabile!)  name: twelve-factor-demo-ingress

  type: LoadBalancer  annotations:

```    kubernetes.io/ingress.class: "nginx"

spec:

**Perché è importante:**  rules:

- ✅ L'app include il web server (Tomcat embedded)  - host: twelve-factor.local

- ✅ Porta configurabile tramite `SERVER_PORT` env var    http:

- ✅ Self-contained, non richiede server esterno      paths:

      - path: /

---        pathType: Prefix

        backend:

## 🔧 Operazioni Common          service:

            name: twelve-factor-demo-service

### Scaling (Factor VIII)            port:

              number: 80

```bash```

# Scala orizzontalmente a 3 istanze

kubectl scale deployment/twelve-factor-demo --replicas=3---



# Verifica scaling## 🧩 6. Comandi di Deploy

kubectl get pods -w

```bash

# Auto-scaling (opzionale)# Assicurati che Kubernetes sia abilitato in Docker Desktop

kubectl autoscale deployment/twelve-factor-demo \kubectl apply -f k8s/configmap.yaml

  --cpu-percent=70 \kubectl apply -f k8s/secret.yaml

  --min=2 \kubectl apply -f k8s/deployment.yaml

  --max=10kubectl apply -f k8s/service.yaml

```kubectl apply -f k8s/ingress.yaml   # raccomandato per Docker Desktop

```

### Aggiornamento Configurazione (Factor III)

Verifica lo stato:

```bash

# Modifica ConfigMap```bash

kubectl edit configmap twelve-factor-demo-configkubectl get pods

kubectl get svc

# Riavvia deployment per applicarekubectl get deployments

kubectl rollout restart deployment/twelve-factor-demokubectl get ingress

```

# Oppure modifica direttamente il file e riapplica

kubectl apply -f k8s/configmap.yaml---

```

## 🧪 7. Test dell'applicazione con Docker Desktop

### Rolling Update (Factor V: Build, Release, Run)

### Opzione 1: Usando Ingress (raccomandata)

```bash

# Nuova versione dell'immagine1. **Abilita Ingress Controller in Docker Desktop:**

docker build -t twelve-factor-demo:1.1.0 .    - Vai su Docker Desktop → Settings → Kubernetes

    - Seleziona "Enable Ingress Controller"

# Update deployment con nuova immagine

kubectl set image deployment/twelve-factor-demo \2. **Aggiungi al file `/etc/hosts` (macOS/Linux) o `C:\Windows\System32\drivers\etc\hosts` (Windows):**

  twelve-factor-demo=twelve-factor-demo:1.1.0   ```

   127.0.0.1 twelve-factor.local

# Monitora rollout   ```

kubectl rollout status deployment/twelve-factor-demo

3. **Accedi all'applicazione:**

# Verifica history   ```

kubectl rollout history deployment/twelve-factor-demo   http://twelve-factor.local

   ```

# Rollback se necessario

kubectl rollout undo deployment/twelve-factor-demo### Opzione 2: Port Forwarding (alternativa)

```

```bash

### Monitoring e Logs (Factor XI, XII)kubectl port-forward service/twelve-factor-demo-service 8080:80

```

```bash

# Logs in real-time (Factor XI)Poi visita: `http://localhost:8080`

kubectl logs -f deployment/twelve-factor-demo

---

# Logs di tutti i pod

kubectl logs -l app=twelve-factor-demo --tail=50## 🔁 8. Rollout, Scaling e Self-Healing



# Health check (Factor XII)Aggiornamento dell'immagine:

kubectl exec -it deployment/twelve-factor-demo -- \

  curl http://localhost:8080/actuator/health```bash

kubectl set image deployment/twelve-factor-demo twelve-factor-demo=alf/twelve-factor-demo:1.0.1

# Metriche Prometheus (Factor XII)kubectl rollout status deployment/twelve-factor-demo

kubectl port-forward service/twelve-factor-demo-service 8080:8080```

curl http://localhost:8080/actuator/prometheus | grep jvm

```Rollback:



---```bash

kubectl rollout undo deployment/twelve-factor-demo

## 🧪 Test dei 12 Fattori su Kubernetes```



### Factor I: CodebaseScaling:

```bash

# Stessa immagine, deployment multipli```bash

kubectl create namespace devkubectl scale deployment/twelve-factor-demo --replicas=4

kubectl create namespace prod```



kubectl apply -f k8s/ -n dev---

kubectl apply -f k8s/ -n prod

## 📈 9. Diagramma Concettuale (Mermaid)

# Config diversa per namespace (ConfigMap differenti)

``````mermaid

flowchart LR

### Factor III: Config  subgraph "Docker Desktop Kubernetes"

```bash    subgraph Pod1

# Cambia config senza rebuild      A[Spring Boot Container - twelve-factor-demo]

kubectl set env deployment/twelve-factor-demo GREETING_PREFIX="Ciao"    end

    subgraph Pod2

# Verifica      B[Spring Boot Container - twelve-factor-demo]

curl http://localhost:8080/api/v1/greeting?name=World    end

```    S[Service] --> A

    S --> B

### Factor VI: Processes (Stateless)    I[Ingress Controller] --> S

```bash  end

# Elimina un pod, il traffico va automaticamente agli altri  U[Utente / Browser] --> I

kubectl delete pod <pod-name>```



# Nessuna perdita di sessione (perché stateless!)---

```

## 🧰 10. Script di Deploy automatico per Docker Desktop

### Factor VIII: Concurrency

```bash**deploy.sh**

# Load test con scalabilità

kubectl scale deployment/twelve-factor-demo --replicas=10```bash

#!/bin/bash

# Ogni pod gestisce richieste indipendentementeset -e

ab -n 10000 -c 100 http://localhost:8080/api/v1/greeting?name=Load

```echo "🔧 Verifico che Docker Desktop Kubernetes sia attivo..."

kubectl cluster-info

### Factor IX: Disposability

```bashecho "🚀 Applying Kubernetes manifests..."

# Simula crash e verifica auto-healingkubectl apply -f k8s/configmap.yaml

kubectl delete pod <pod-name>kubectl apply -f k8s/secret.yaml

kubectl apply -f k8s/deployment.yaml

# Kubernetes riavvia automaticamente il podkubectl apply -f k8s/service.yaml

kubectl get pods -wkubectl apply -f k8s/ingress.yaml

```

echo "⏳ Attendo che i pod siano ready..."

### Factor XI: Logskubectl wait --for=condition=ready pod -l app=twelve-factor-demo --timeout=60s

```bash

# Log centralizzati via Kubernetesecho "✅ Deployment completato!"

kubectl logs deployment/twelve-factor-demo --all-containers=trueecho ""

echo "📊 Stato del deployment:"

# Integrazione con log aggregator (es. ELK, Fluentd)kubectl get pods -l app=twelve-factor-demo

```echo ""

echo "🌐 Servizi:"

### Factor XII: Admin Processeskubectl get svc twelve-factor-demo-service

```bashecho ""

# Esegui task admin come Job one-offecho "🔗 Ingress:"

kubectl run migration --image=twelve-factor-demo:1.0.0 \kubectl get ingress twelve-factor-demo-ingress

  --restart=Never \echo ""

  --rm -it \echo "🎯 Per testare l'applicazione:"

  --env="MIGRATION_TASK=true" \echo "1. Assicurati di avere questa riga in /etc/hosts:"

  -- java -jar app.jar --migrateecho "   127.0.0.1 twelve-factor.local"

echo "2. Visita: http://twelve-factor.local"

# Oppure exec in pod esistente```

kubectl exec -it deployment/twelve-factor-demo -- /bin/bash

```---



---## 🔧 Configurazione aggiuntiva per Docker Desktop



## 📊 Monitoring Dashboard### Verifica dell'ambiente Docker Desktop



### Setup Prometheus + Grafana (opzionale)```bash

# Verifica che Kubernetes sia attivo

```bashkubectl cluster-info

# Installa Prometheus Operator

kubectl create namespace monitoring# Verifica i nodi

helm install prometheus prometheus-community/kube-prometheus-stack \kubectl get nodes

  --namespace monitoring

# Verifica i namespace

# Port forward Grafanakubectl get namespaces

kubectl port-forward -n monitoring svc/prometheus-grafana 3000:80```



# Login: admin / prom-operator### Pulizia delle risorse

# Importa dashboard Spring Boot (ID: 12835)

``````bash

# Elimina tutte le risorse del progetto

### Metriche Disponibili (Factor XII)kubectl delete -f k8s/



- `http_server_requests_seconds` - Request latency# Oppure elimina singolarmente

- `jvm_memory_used_bytes` - JVM memory usagekubectl delete deployment twelve-factor-demo

- `jvm_gc_pause_seconds` - Garbage collectionkubectl delete service twelve-factor-demo-service

- `process_cpu_usage` - CPU usagekubectl delete ingress twelve-factor-demo-ingress

- Custom: `greeting_created_total` (da implementare)kubectl delete configmap twelve-factor-demo-config

kubectl delete secret twelve-factor-demo-secret

---```



## 🔐 Security Best Practices### Monitoraggio in tempo reale



### Secrets Management (Factor III)```bash

# Monitora i pod

```bashkubectl get pods -w

# NON committare secrets nel Git!

# Usa Sealed Secrets o External Secrets Operator# Log in tempo reale

kubectl logs -f deployment/twelve-factor-demo

# Esempio con kubectl

kubectl create secret generic db-credentials \# Descrizione dettagliata

  --from-literal=username=postgres \kubectl describe deployment twelve-factor-demo

  --from-literal=password=super-secret```

# Usa nel deployment
env:
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: password
```

### Security Context

Il deployment include già security best practices:

```yaml
securityContext:
  runAsNonRoot: true  # Non eseguire come root
  runAsUser: 1000     # UID non privilegiato
```

---

## 🧹 Cleanup

```bash
# Elimina tutte le risorse
kubectl delete -f k8s/

# Oppure elimina singolarmente
kubectl delete deployment twelve-factor-demo
kubectl delete service twelve-factor-demo-service
kubectl delete configmap twelve-factor-demo-config
kubectl delete secret twelve-factor-demo-secret
kubectl delete ingress twelve-factor-demo-ingress
```

---

## 📚 Riferimenti

- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
- [12-Factor App](https://12factor.net/)
- [Prometheus Metrics](https://prometheus.io/docs/introduction/overview/)
