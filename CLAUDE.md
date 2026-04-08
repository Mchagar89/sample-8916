# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Run locally
./run.sh                        # wraps: mvn clean spring-boot:run
curl http://localhost:8080/api/hello

# Build jar
mvn clean package -DskipTests

# Compile only
mvn clean test-compile

# Run tests
mvn clean test
```

## Architecture

Minimal Spring Boot 3.2.6 microservice targeting Java 21.

- `com.example.demo.DemoApplication` — entry point
- `com.example.demo.HelloController` — single REST endpoint: `GET /api/hello`
- Actuator exposed at `/actuator/health` and `/actuator/info`
- Server port: 8080

## Infrastructure

**Docker**: Multi-stage build (Maven 3.9.8 builder → `temurin-17-jre-jammy` runtime). Output jar: `target/demo-0.0.1-SNAPSHOT.jar`.

**Kubernetes** (`k8s/`): 2 replicas, LoadBalancer service (port 80 → 8080), liveness/readiness probes on `/actuator/health`.

**CI/CD** (`.github/workflows/aws-eks-deploy.yml`):
- Push to `main` → deploys to `demo-production` namespace
- Push to `stage` → deploys to `demo-staging` namespace
- Manual dispatch supports environment override
- Rollback job available (manual trigger only): `kubectl rollout undo`
- Required GitHub secrets: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `AWS_ACCOUNT_ID`, `EKS_CLUSTER_NAME`

ECR lifecycle policy keeps latest 10 tagged images, expires untagged after 30 days.
