Per installare Kubernetes Dashboard su Docker Desktop, ecco la guida completa:

## 1. Attiva Kubernetes su Docker Desktop

1. Apri Docker Desktop
2. Vai su Settings (ingranaggio in alto a destra)
3. Sezione "Kubernetes"
4. ✅ **Enable Kubernetes**
5. Clicca "Apply & Restart"

## 2. Installa Kubernetes Dashboard

Apri il terminale e esegui questi comandi:

### **Deploy della Dashboard:**
```bash
# Applica i manifest ufficiali della Dashboard
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml
```

## 3. Crea un Service Account per l'accesso

Crea un file `dashboard-adminuser.yaml`:

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: admin-user
  namespace: kubernetes-dashboard
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: admin-user
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
- kind: ServiceAccount
  name: admin-user
  namespace: kubernetes-dashboard
```

Applica il file:
```bash
kubectl apply -f dashboard-adminuser.yaml
```

## 4. Ottieni il Token di accesso

```bash
# Ottieni il token
kubectl -n kubernetes-dashboard create token admin-user
```

**Salva il token che viene visualizzato** - ti servirà per accedere.

## 5. Accedi alla Dashboard

### **Metodo 1: Port Forward (più semplice)**
```bash
kubectl proxy
```

Poi apri il browser su:
```
http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
```

### **Metodo 2: NodePort (alternativo)**
Modifica il service per esporlo su NodePort:

```bash
kubectl patch svc kubernetes-dashboard -n kubernetes-dashboard -p '{"spec": {"type": "NodePort"}}'
```

Trova la porta:
```bash
kubectl get svc -n kubernetes-dashboard
```

Accedi su: `https://localhost:<PORTA>`

## 6. Accesso alla Dashboard

1. Seleziona **"Token"**
2. Incolla il token ottenuto al passo 4
3. Clicca "Sign in"

## 7. Script di installazione rapido

Crea un file `install-dashboard.sh`:

```bash
#!/bin/bash

# Abilita Kubernetes su Docker Desktop (da fare manualmente)
echo "Assicurati di aver abilitato Kubernetes in Docker Desktop Settings"

# Deploy Dashboard
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml

# Crea admin user
kubectl apply -f - <<EOF
apiVersion: v1
kind: ServiceAccount
metadata:
  name: admin-user
  namespace: kubernetes-dashboard
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: admin-user
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
- kind: ServiceAccount
  name: admin-user
  namespace: kubernetes-dashboard
EOF

# Ottieni token
echo "=== TOKEN DI ACCESSO ==="
kubectl -n kubernetes-dashboard create token admin-user

echo "=== PER ACCEDERE ==="
echo "1. Esegui: kubectl proxy"
echo "2. Vai su: http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/"
echo "3. Usa il token sopra"
```

## 8. Verifica l'installazione

```bash
# Verifica che tutto sia running
kubectl get pods -n kubernetes-dashboard

# Dovresti vedere qualcosa tipo:
# NAME                                        READY   STATUS    RESTARTS   AGE
# dashboard-metrics-scraper-5f6c6c16d-xxxxx   1/1     Running   0          1m
# kubernetes-dashboard-7c6d6f8f55-xxxxx       1/1     Running   0          1m
```

## 9. Comandi utili per la gestione

```bash
# Cancellare la dashboard
kubectl delete -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml

# Riavvio della dashboard
kubectl rollout restart deployment kubernetes-dashboard -n kubernetes-dashboard

# Logs della dashboard
kubectl logs -l k8s-app=kubernetes-dashboard -n kubernetes-dashboard
```

## Note importanti:

- **Sicurezza**: La dashboard è esposta solo localmente via `kubectl proxy`
- **Token**: Il token scade, puoi rigenerarlo quando serve
- **Risorse**: Kubernetes Dashboard consuma poche risorse
- **Aggiornamento**: Per aggiornare, elimina e re-installa con la versione più recente

Ora hai una Kubernetes Dashboard completamente funzionante su Docker Desktop! 🎉