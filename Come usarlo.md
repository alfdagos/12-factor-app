# Build
mvn clean package

# Docker build
docker build -t twelve-factor-demo:1.0.0 .

# Run
docker run -p 8080:8080 -e GREETING_PREFIX="Ciao" twelve-factor-demo:1.0.0

# Test
curl http://localhost:8080/api/v1/greeting?name=Cloud