# Deployment & DevOps Guide

**Version:** 1.0  
**Last Updated:** June 7, 2026  
**Target Audience:** DevOps engineers, system administrators, deployment leads  

---

## Table of Contents

1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [Local Development Setup](#local-development-setup)
3. [Staging Environment](#staging-environment)
4. [Production Deployment](#production-deployment)
5. [Docker Containerization](#docker-containerization)
6. [Kubernetes Deployment](#kubernetes-deployment)
7. [CI/CD Pipeline](#cicd-pipeline)
8. [Rollback Procedures](#rollback-procedures)
9. [Monitoring & Logging](#monitoring--logging)
10. [Troubleshooting](#troubleshooting)

---

## Pre-Deployment Checklist

### Code Quality & Testing

```markdown
## Code Quality
- [ ] All unit tests pass (mvn test)
- [ ] All integration tests pass (mvn verify)
- [ ] Code coverage > 80%
- [ ] SonarQube analysis passed
  - [ ] No critical issues
  - [ ] No major code smells
  - [ ] Duplication < 5%
- [ ] OWASP security scan passed
- [ ] No SQL injection vulnerabilities
- [ ] No hardcoded credentials

## Performance
- [ ] Load testing completed (target: 1000 req/sec)
- [ ] Database indexing optimized
- [ ] Query performance verified (< 200ms target)
- [ ] Connection pooling configured
- [ ] Cache strategy implemented
- [ ] Response times verified

## Security
- [ ] All passwords hashed with BCrypt
- [ ] JWT secrets configured and rotated
- [ ] CORS properly configured
- [ ] SQL injection prevention verified
- [ ] XSS protection enabled
- [ ] CSRF tokens implemented
- [ ] Rate limiting configured
- [ ] TLS/SSL certificates valid
- [ ] Secrets management configured

## Configuration
- [ ] Environment variables documented
- [ ] Database migrations tested
- [ ] Logging configured for production
- [ ] Error messages sanitized (no sensitive data)
- [ ] Debug mode disabled
- [ ] API documentation updated
- [ ] README updated with deployment info

## Documentation
- [ ] Architecture documentation complete
- [ ] API documentation generated
- [ ] Database schema documented
- [ ] Deployment guide prepared
- [ ] Rollback procedures documented
- [ ] Monitoring setup documented
- [ ] Troubleshooting guide created

## Infrastructure
- [ ] Servers provisioned and tested
- [ ] SSL certificates installed
- [ ] Firewall rules configured
- [ ] Load balancer configured
- [ ] Database backups tested
- [ ] Monitoring alerts configured
- [ ] Log aggregation setup
```

---

## Local Development Setup

### Development Environment

```bash
# System Requirements
Java:              JDK 17 (LTS)
Maven:             3.8+
PostgreSQL:        12+
Git:               2.30+
Docker (Optional): 20.10+

# Installation Steps

## 1. Clone Repository
git clone <repository-url>
cd parking-system
git checkout main  # or appropriate branch

## 2. Setup PostgreSQL
# Create database
psql -U postgres
CREATE DATABASE parking_system_db;
CREATE USER parking_admin WITH PASSWORD 'dev_password123';
GRANT ALL PRIVILEGES ON DATABASE parking_system_db TO parking_admin;
\q

# Apply migrations
psql -U parking_admin -d parking_system_db -f DATABASE_MIGRATION.sql

## 3. Configure Application
# Create application-dev.properties
cp src/main/resources/application.properties \
   src/main/resources/application-dev.properties

# Edit with local settings
spring.datasource.url=jdbc:postgresql://localhost:5432/parking_system_db
spring.datasource.username=parking_admin
spring.datasource.password=dev_password123

## 4. Build and Run
mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

## 5. Verify
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}

## 6. Run Tests
mvn test
mvn verify
```

---

## Staging Environment

### Staging Setup Architecture

```
┌─────────────────────────────────────────┐
│          STAGING ENVIRONMENT             │
├─────────────────────────────────────────┤
│                                          │
│  Load Balancer (SSL)                    │
│  └─ https://staging.parking.com         │
│          │                               │
│    ┌─────┴──────────────────┐           │
│    │                        │            │
│    ▼ Instance 1         Instance 2  ▼   │
│  ┌──────────┐          ┌──────────┐    │
│  │Parking   │          │Parking   │    │
│  │System:   │          │System:   │    │
│  │8080      │          │8080      │    │
│  └──────────┘          └──────────┘    │
│         │                       │        │
│         └───────────┬───────────┘        │
│                     │                    │
│                     ▼                    │
│          ┌──────────────────┐           │
│          │ PostgreSQL 12    │           │
│          │ (Staging DB)     │           │
│          │ Master           │           │
│          └──────────────────┘           │
│                  │                       │
│                  ▼                       │
│          ┌──────────────────┐           │
│          │ PostgreSQL 12    │           │
│          │ (Read Replica)   │           │
│          └──────────────────┘           │
│                                          │
│  Monitoring & Logging:                  │
│  ├─ Application Logs (JSON fomat)      │
│  ├─ Error Tracking (Sentry/ELK)        │
│  ├─ Metrics (Prometheus)                │
│  └─ Dashboards (Grafana)                │
│                                          │
└─────────────────────────────────────────┘
```

### Staging Deployment Steps

```bash
# 1. Build Docker Image
docker build -t parking-system:staging-$(date +%Y%m%d) .
docker tag parking-system:staging-$(date +%Y%m%d) \
           registry.example.com/parking-system:staging-latest

# 2. Push to Registry
docker push registry.example.com/parking-system:staging-latest

# 3. Deploy to Staging (using Docker Compose or K8s)
# Using Docker Compose:
docker-compose -f docker-compose.staging.yml up -d

# Using Kubernetes:
kubectl set image deployment/parking-system-staging \
  parking-system=registry.example.com/parking-system:staging-latest \
  -n staging --record

# 4. Run Database Migrations
kubectl exec -it <pod-name> -n staging -- \
  java -cp classpath:. org.flywaydb.core.Flyway -url=... migrate

# 5. Smoke Tests
curl -X POST https://staging.parking.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass"}'

# 6. Monitor Deployment
kubectl rollout status deployment/parking-system-staging -n staging
kubectl logs -f -n staging -l app=parking-system
```

---

## Production Deployment

### Production Environment Architecture

```
┌──────────────────────────────────────────────────┐
│           PRODUCTION ENVIRONMENT                  │
├──────────────────────────────────────────────────┤
│                                                  │
│  CDN (CloudFront/Cloudflare)                    │
│          │                                       │
│          ▼                                       │
│  ┌────────────────────┐                         │
│  │  API Gateway       │                         │
│  │  (Rate Limiting)   │                         │
│  └────────┬───────────┘                         │
│           │                                      │
│    ┌──────┴───────────────────┐                │
│    │                          │                 │
│    ▼ LB Primary         LB Secondary ▼          │
│  ┌─────────────────────────────────────┐       │
│  │                                     │        │
│  │  ┌───────┐  ┌───────┐  ┌───────┐  │        │
│  │  │  Pod  │  │  Pod  │  │  Pod  │  │ Min 3  │
│  │  │  #1   │  │  #2   │  │  #3   │  │        │
│  │  └───────┘  └───────┘  └───────┘  │        │
│  │                                     │        │
│  └────────┬────────────────────────────┘       │
│           │                                     │
│           ▼                                     │
│  ┌─────────────────────────────────────┐       │
│  │   PostgreSQL RDS (Multi-AZ)         │       │
│  │   Primary (Write)                   │       │
│  └─────────────────────────────────────┘       │
│           │                                     │
│           ├─────────────────┐                  │
│           ▼                 ▼                  │
│   ┌──────────────┐   ┌──────────────┐        │
│   │ Read Replica │   │ Read Replica │        │
│   │ (Region 1)   │   │ (Region 2)   │        │
│   └──────────────┘   └──────────────┘        │
│                                               │
│  Caching Layer:                              │
│  ├─ Redis (Session Cache)                    │
│  ├─ Redis (Query Cache)                      │
│  └─ Application-level Cache                  │
│                                               │
│  Security:                                   │
│  ├─ VPC with private subnets                │
│  ├─ Security Groups (Firewall)              │
│  ├─ WAF (Web Application Firewall)          │
│  ├─ DDoS Protection                         │
│  └─ VPN Access                              │
│                                               │
│  Monitoring & Observability:                 │
│  ├─ Cloudwatch / ELK Stack                  │
│  ├─ Prometheus + Grafana                    │
│  ├─ Distributed Tracing (Jaeger)            │
│  ├─ APM (New Relic / DataDog)               │
│  ├─ Error Tracking (Sentry)                 │
│  └─ Health Checks & Alerts                  │
│                                               │
└──────────────────────────────────────────────────┘
```

### Production Deployment Procedure

```bash
#!/bin/bash
# Production deployment script with safety checks

set -e  # Exit on error

# Configuration
ENVIRONMENT="production"
SERVICE_NAME="parking-system"
IMAGE_TAG="v$(date +%Y%m%d-%H%M%S)"
REGISTRY="registry.example.com"
NAMESPACE="production"
REPLICAS=3

echo "=== Starting Production Deployment ==="
echo "Environment: $ENVIRONMENT"
echo "Image Tag: $IMAGE_TAG"

# Step 1: Pre-deployment checks
echo "Step 1: Pre-deployment checks..."
# Check database connectivity
kubectl exec -it <pod> -n $NAMESPACE -- \
  pg_isready -h $DB_HOST -p 5432

# Check application health
for i in {1..10}; do
  if curl -f http://localhost:8080/actuator/health; then
    echo "Application health: OK"
    break
  fi
  sleep 5
done

# Step 2: Build and push image
echo "Step 2: Building Docker image..."
docker build \
  --build-arg VERSION=$IMAGE_TAG \
  --tag $REGISTRY/$SERVICE_NAME:$IMAGE_TAG .
docker tag $REGISTRY/$SERVICE_NAME:$IMAGE_TAG \
           $REGISTRY/$SERVICE_NAME:latest
docker push $REGISTRY/$SERVICE_NAME:$IMAGE_TAG
docker push $REGISTRY/$SERVICE_NAME:latest

# Step 3: Update deployment
echo "Step 3: Updating Kubernetes deployment..."
kubectl set image deployment/$SERVICE_NAME \
  $SERVICE_NAME=$REGISTRY/$SERVICE_NAME:$IMAGE_TAG \
  -n $NAMESPACE \
  --record

# Step 4: Monitor rollout
echo "Step 4: Monitoring rollout..."
kubectl rollout status deployment/$SERVICE_NAME \
  -n $NAMESPACE \
  --timeout=5m

# Step 5: Run smoke tests
echo "Step 5: Running smoke tests..."
TEST_URL="https://api.parking.com"

# Test authentication
curl -X POST $TEST_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass"}' || \
  { echo "Login test failed"; exit 1; }

# Test parking operations
curl -X GET $TEST_URL/api/parkingspace/all \
  -H "Authorization: Bearer $TEST_TOKEN" || \
  { echo "Parking space fetch failed"; exit 1; }

echo "=== Smoke tests passed ==="

# Step 6: Verify metrics
echo "Step 6: Verifying application metrics..."
# Check error rate < 1%
# Check response time < 200ms
# Check CPU usage < 70%

echo "=== Production Deployment Complete ==="
echo "Image Tag: $IMAGE_TAG"
echo "Deployment: $(date)"
```

---

## Docker Containerization

### Dockerfile

```dockerfile
# Multi-stage build for optimized image
FROM maven:3.8-openjdk-17 AS builder

# Build arguments
ARG VERSION=1.0.0

# Copy source
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim

# Metadata
LABEL maintainer="DevOps Team"
LABEL version="${VERSION}"
LABEL description="Parking System Backend"

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app user (non-root)
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy JAR from builder
COPY --from=builder --chown=appuser:appuser /app/target/Parking-System-*.jar app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Environment variables (override at runtime)
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=production

# Run application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
```

### Docker Compose

```yaml
# docker-compose.yml - Local development
version: '3.8'

services:
  postgres:
    image: postgres:15
    container_name: parking-db
    environment:
      POSTGRES_DB: parking_system_db
      POSTGRES_USER: parking_admin
      POSTGRES_PASSWORD: dev_password123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./DATABASE_MIGRATION.sql:/docker-entrypoint-initdb.d/schema.sql
    networks:
      - parking-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U parking_admin"]
      interval: 10s
      timeout: 5s
      retries: 5

  parking-app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: parking-system
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/parking_system_db
      SPRING_DATASOURCE_USERNAME: parking_admin
      SPRING_DATASOURCE_PASSWORD: dev_password123
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate
      JWT_SECRET: ${JWT_SECRET:-dev-secret-key-32-chars-minimum}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - parking-network

  redis:
    image: redis:7-alpine
    container_name: parking-cache
    ports:
      - "6379:6379"
    networks:
      - parking-network

volumes:
  postgres_data:

networks:
  parking-network:
    driver: bridge
```

---

## Kubernetes Deployment

### Kubernetes Manifests

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: parking-system
  namespace: production
  labels:
    app: parking-system
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: parking-system
  template:
    metadata:
      labels:
        app: parking-system
        version: v1
    spec:
      # Pod scheduling
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - parking-system
              topologyKey: kubernetes.io/hostname
      
      # Service account
      serviceAccountName: parking-system
      
      # Security context
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      
      containers:
      - name: parking-system
        image: registry.example.com/parking-system:latest
        imagePullPolicy: IfNotPresent
        
        # Port
        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        
        # Environment variables
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        
        # Resource limits and requests
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        
        # Health checks
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: http
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 3
          failureThreshold: 3
        
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: http
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        
        # Volume mounts
        volumeMounts:
        - name: config
          mountPath: /app/config
          readOnly: true
      
      # Volumes
      volumes:
      - name: config
        configMap:
          name: parking-system-config
      
      # Image pull secrets
      imagePullSecrets:
      - name: registry-credentials

---
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: parking-system
  namespace: production
  labels:
    app: parking-system
spec:
  type: LoadBalancer
  selector:
    app: parking-system
  ports:
  - name: http
    port: 80
    targetPort: http
    protocol: TCP
  - name: https
    port: 443
    targetPort: http
    protocol: TCP

---
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: parking-system-config
  namespace: production
data:
  application.properties: |
    spring.jpa.hibernate.ddl-auto=validate
    logging.level.com.parkingSystem=INFO
    server.compression.enabled=true
```

---

## CI/CD Pipeline

### GitHub Actions Pipeline

```yaml
# .github/workflows/deploy.yml
name: Build and Deploy

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

env:
  REGISTRY: registry.example.com
  IMAGE_NAME: parking-system

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    
    - name: Run tests
      run: mvn clean verify
      env:
        SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/parking_system_db
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    
    permissions:
      contents: read
      packages: write
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    
    - name: Build JAR
      run: mvn clean package -DskipTests
    
    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v2
    
    - name: Login to Registry
      uses: docker/login-action@v2
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ secrets.REGISTRY_USERNAME }}
        password: ${{ secrets.REGISTRY_PASSWORD }}
    
    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v4
      with:
        images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
        tags: |
          type=ref,event=branch
          type=sha,prefix={{branch}}-
          type=semver,pattern={{version}}
    
    - name: Build and push Docker image
      uses: docker/build-push-action@v4
      with:
        context: .
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}
        cache-from: type=registry,ref=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:buildcache
        cache-to: type=registry,ref=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:buildcache,mode=max

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Deploy to Production
      run: |
        echo "Deploying to production cluster..."
        # kubectl configuration and deployment commands
```

---

## Rollback Procedures

### Zero-Downtime Rollback

```bash
#!/bin/bash
# rollback.sh - Rollback to previous deployment

set -e

NAMESPACE=${1:-production}
DEPLOYMENT=${2:-parking-system}

echo "=== Starting Rollback Process ==="
echo "Namespace: $NAMESPACE"
echo "Deployment: $DEPLOYMENT"

# Get rollout history
echo "Recent deployment history:"
kubectl rollout history deployment/$DEPLOYMENT -n $NAMESPACE | head -5

# Get previous revision
PREVIOUS_REVISION=$(kubectl rollout history deployment/$DEPLOYMENT -n $NAMESPACE | tail -2 | head -1 | awk '{print $1}')

echo "Rolling back to revision: $PREVIOUS_REVISION"

# Execute rollback
kubectl rollout undo deployment/$DEPLOYMENT \
  --to-revision=$PREVIOUS_REVISION \
  -n $NAMESPACE

# Wait for rollback
kubectl rollout status deployment/$DEPLOYMENT \
  -n $NAMESPACE \
  --timeout=5m

# Verify
echo "Verifying rollback..."
kubectl get deployment/$DEPLOYMENT -n $NAMESPACE -o wide
kubectl get pods -n $NAMESPACE -l app=$DEPLOYMENT

# Smoke tests
echo "Running smoke tests..."
curl -X GET https://api.parking.com/api/parkingspace/all \
  -H "Authorization: Bearer $TEST_TOKEN"

echo "=== Rollback Complete ==="
```

---

## Monitoring & Logging

### Prometheus & Grafana Setup

```yaml
# prometheus-config.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'parking-system'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - production
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        action: keep
        regex: parking-system
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__
```

### ELK Stack for Centralized Logging

```yaml
# logstash-config.conf
input {
  beats {
    port => 5000
  }
}

filter {
  if [type] == "parking-system" {
    grok {
      match => { "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{USER:logger} %{DATA:message}" }
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "parking-system-%{+YYYY.MM.dd}"
  }
}
```

---

## Troubleshooting

### Common Deployment Issues

```markdown
## Pod stuck in ImagePullBackOff
- Check image registry credentials
- Verify image exists in registry
- Check network access to registry
  
  Fix:
  kubectl create secret docker-registry regcred \
    --docker-server=<registry> \
    --docker-username=<user> \
    --docker-password=<pass>

## Pod OOMKilled
- Memory limit too low for workload
- Memory leak in application
  
  Fix:
  kubectl set resources deployment parking-system \
    --limits=memory=2Gi \
    -n production

## Service not accessible
- Check service selector matches pod labels
- Verify security groups/network policies
- Check DNS resolution
  
  Debug:
  kubectl get svc -o wide
  kubectl get endpoints
  kubectl logs deployment/parking-system

## High latency observed
- Database connection issues
- Insufficient replicas (scale up)
- Resource contention
  
  Monitor:
  kubectl top nodes
  kubectl top pods -n production
  kubectl describe node <node-name>
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-07 | Initial deployment guide |

---

**Document Prepared By:** DevOps Team  
**Last Updated:** 2026-06-07  
**Next Review:** 2026-09-07

**End of Document**

