#!/bin/bash
set -e

echo "🔧 Verifico che Docker Desktop Kubernetes sia attivo..."
kubectl cluster-info

echo "🚀 Applying Kubernetes manifests..."
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml

echo "⏳ Attendo che i pod siano ready..."
kubectl wait --for=condition=ready pod -l app=twelve-factor-demo --timeout=60s

echo "✅ Deployment completato!"
echo ""
echo "📊 Stato del deployment:"
kubectl get pods -l app=twelve-factor-demo
echo ""
echo "🌐 Servizi:"
kubectl get svc twelve-factor-demo-service
echo ""
echo "🔗 Ingress:"
kubectl get ingress twelve-factor-demo-ingress
echo ""
echo "🎯 Per testare l'applicazione:"
echo "1. Assicurati di avere questa riga in /etc/hosts:"
echo "   127.0.0.1 twelve-factor.local"
echo "2. Visita: http://twelve-factor.local"