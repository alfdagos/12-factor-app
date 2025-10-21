## Quick context for AI coding agents

This is a small Spring Boot demo that demonstrates the 12-Factor App principles. The app is intentionally minimal:

- Java + Spring Boot (see `pom.xml`)
- Single HTTP service exposes endpoints under `/api/v1` (`GreetingController`)
- Business logic lives in `src/main/java/it/alf/twelve_factor/service/GreetingService.java`
- Config via `src/main/resources/application.yml` using environment variables
- Docker multi-stage build in `Dockerfile` and Kubernetes manifests under `k8s/`

When making changes, prefer to update configuration via environment variables or `k8s/` ConfigMaps rather than hardcoding values.

## What to know about the code & architecture

- Entry point: `it.alf.twelve_factor.Application` — standard Spring Boot app.
- HTTP controllers: `src/main/java/it/alf/twelve_factor/controller/*` (port binding at server.port, default 8080).
- Services: `src/main/java/it/alf/twelve_factor/service/*` — stateless business logic; avoid introducing in-memory session state.
- DTOs/models: `src/main/java/it/alf/twelve_factor/model/*` (POJOs using Lombok).
- Logging: uses SLF4J via Lombok `@Slf4j` and writes to stdout (see `application.yml`).

Design intent to preserve:
- Keep processes stateless (Factor VI). If state is required, prefer backing services.
- Configuration must be injectable from environment (Factor III). Use `@Value("${...}")` or `Environment` lookups.
- Builds are separated from runtime (multi-stage Dockerfile). Avoid embedding environment-specific config in code.

## Developer workflows (commands agents can suggest or use)

- Build jar locally: `mvn clean package -DskipTests`
- Run locally via Maven: `mvn spring-boot:run`
- Build Docker image (multi-stage): `docker build -t twelve-factor-demo:local .`
- Run image: `docker run -p 8080:8080 -e GREETING_PREFIX=Hi twelve-factor-demo:local`
- Kubernetes: manifests in `k8s/` (Deployment, Service, Ingress, ConfigMap/Secret examples). Use `kubectl apply -f k8s/`.

Note: The `Dockerfile` performs a full Maven build in the builder stage and copies the produced JAR into a runtime image. Prefer changing build logic in `Dockerfile` and `pom.xml` rather than adding ad-hoc scripts.

## Conventions & patterns found in this repo

- Project is Maven-based; Spring Boot parent controls dependency versions (see `pom.xml`). Do not add duplicate version tags unless necessary.
- Lombok is used for model classes and constructors — the codebase expects annotation processing during compile.
- Metrics & health endpoints are exposed via Spring Actuator (configured in `application.yml`). Use `/actuator/health` for readiness/liveness.
- Environment variables are used in `application.yml` with fallbacks: `${VAR:default}`. Mirror that style when adding newConfig keys.

## Integration points and external dependencies

- Actuator + Micrometer (Prometheus) — configured in `pom.xml` and `application.yml`.
- Container runtime: Docker image built by `Dockerfile` and deployed to Kubernetes (manifests under `k8s/`).
- No external databases are wired in the default code, but the repo demonstrates patterns for datasource/redis via env vars (see README sections and `application.yml`).

## Typical small tasks an agent should follow

1. For a behavioral fix in the service layer, update `GreetingService` and add/adjust unit tests (project currently has no tests — add JUnit 5 + Spring Boot test if adding tests).
2. When changing config keys, update `application.yml` and `k8s/configmap.yaml` (or `k8s/` README) to keep runtime parity.
3. When adding dependencies, update `pom.xml` and prefer adding them without explicit versions unless a different version is required (parent POM manages versions).
4. For Docker-related updates, edit `Dockerfile` to keep the multi-stage build pattern; update `k8s/deployment.yaml` image name when releasing.

## Where to look for examples in the repo

- Controller example: `src/main/java/it/alf/twelve_factor/controller/GreetingController.java`
- Service example: `src/main/java/it/alf/twelve_factor/service/GreetingService.java`
- Config example: `src/main/resources/application.yml`
- Build example: `pom.xml` and `Dockerfile`
- Deployment example: `k8s/deployment.yaml`, `k8s/service.yaml`, `k8s/ingress.yaml`

## Error modes & guardrails for agents

- Do not add long-running background threads or in-memory session maps; the project is designed for stateless pods.
- When touching `pom.xml`, ensure Lombok annotation processing remains intact (maven-compiler-plugin config).
- When changing Java version, update `<java.version>` in `pom.xml` and verify the `Dockerfile` base image supports it.

---

If anything here is unclear or you want the instructions to cover CI/CD specifics or test scaffolding, tell me which area to expand and I will iterate. 
