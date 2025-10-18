# Build con tag versione
docker build -t twelve-factor-demo:1.0.0 .

# Build con tag multiple
docker build -t twelve-factor-demo:1.0.0 -t twelve-factor-demo:latest .

# Build con nome custom del Dockerfile
docker build -f Dockerfile.redhat -t twelve-factor-demo:1.0.0 .

# Build con build arguments
docker build \
  --build-arg APP_VERSION=1.0.0 \
  --build-arg JAVA_VERSION=17 \
  -t twelve-factor-demo:1.0.0 .

# Build senza usare cache (rebuild completo)
docker build --no-cache -t twelve-factor-demo:1.0.0 .

# Build con output progress dettagliato
docker build --progress=plain -t twelve-factor-demo:1.0.0 .

# Build solo di uno stage specifico (utile per debug)
docker build --target builder -t twelve-factor-demo:builder .