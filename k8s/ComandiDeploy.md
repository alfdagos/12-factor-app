# Script di Deployment Kubernetes per Docker Desktop

## Build dell'Immagine Locale

Lo script utilizza Docker per costruire l'immagine localmente:

```bash
docker build -t twelve-factor-demo:1.0.0 .
```

In caso di utilizzo di un namespace differente, è possibile utilizzare:
```bash
docker build -t alf/twelve-factor-demo:1.0.0 .
```

## Deployment delle Risorse Kubernetes

Le risorse Kubernetes vengono applicate nel seguente ordine:

```bash
# Applicazione iniziale di configmap e secret
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# Successiva applicazione di deployment e service
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# L'ingress viene applicato solo se è presente un controller ingress
# kubectl apply -f k8s/ingress.yaml
```

**Nota importante**: Il file `deployment.yaml` deve utilizzare la corretta image pull policy:
```yaml
imagePullPolicy: IfNotPresent
# oppure
imagePullPolicy: Never
```

## Monitoraggio del Deployment

Il rollout del deployment viene monitorato con:
```bash
kubectl rollout status deployment/twelve-factor-demo -w
```

## Verifica delle Risorse

Le risorse deployate possono essere verificate con:
```bash
kubectl get pods -l app=twelve-factor-demo
kubectl get svc twelve-factor-demo-service
kubectl get deployment twelve-factor-demo
```

## Opzioni di Testing

### Opzione 1: Port Forwarding (Raccomandata)
```bash
kubectl port-forward svc/twelve-factor-demo-service 8080:80
```
L'applicazione risulta accessibile all'indirizzo: `http://localhost:8080/api/v1/greeting`

### Opzione 2: Utilizzo di NodePort
Se il servizio è configurato come NodePort:
```bash
# Recupero della porta assegnata
kubectl get svc twelve-factor-demo-service -o jsonpath='{.spec.ports[0].nodePort}'

# Accesso via localhost sulla porta assegnata
curl http://localhost:<NODE_PORT>/api/v1/greeting
```

### Opzione 3: Utilizzo di LoadBalancer
Modificando il tipo di servizio in `service.yaml`:
```yaml
type: LoadBalancer
```
L'accesso avviene via `localhost` sulla porta assegnata.

## Comandi Utili per Docker Desktop
```bash
# Verifica dell'attivazione di Kubernetes
kubectl cluster-info

# Cambio context se necessario
kubectl config use-context docker-desktop

# Verifica dei nodi
kubectl get nodes
```