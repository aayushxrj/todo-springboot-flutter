# Todo Spring Boot + Flutter

Monorepo with a Spring Boot backend, a Spring Boot todos service, and a Flutter web frontend. Auth is handled by Ory Kratos for end users, and service-to-service auth uses Ory Hydra with client credentials. Observability is included via Prometheus + Grafana.

## Prerequisites

- Docker Desktop
- Java 26 (for local Spring Boot development)
- Flutter SDK (for local Flutter development)
- kubectl + minikube (for Kubernetes)
- Helm (for Prometheus + Grafana on Kubernetes)

## Project layout

- todo-springboot/ - Spring Boot backend (API gateway + auth + orchestration)
- todos-list/ - Spring Boot todos service (DB-backed todos + resource server)
- todo_flutter/ - Flutter web frontend
- kratos/ - Ory Kratos config + identity schema
- hydra/ - Ory Hydra config
- docker-compose.yml - Docker Compose stack
- kubernetes/ - Kubernetes manifests
- prometheus/ - Prometheus and Grafana config

## Architecture overview

### Components

- Flutter web (todo_flutter)
  - UI for login, registration, and task management
  - Uses Kratos sessions (X-Session-Token) for auth when calling backend

- Backend (todo-springboot)
  - Terminates user auth by calling Kratos /sessions/whoami
  - Enforces method-level access control
  - Calls todos-list via Feign
  - Gets OAuth2 access tokens from Hydra via client credentials

- Todos service (todos-list)
  - CRUD for tasks using MySQL
  - OAuth2 Resource Server that validates JWTs via Hydra JWKS

- Ory Kratos
  - User identity, login, and registration flows
  - Returns session tokens and identity traits (role)

- Ory Hydra
  - OAuth2 server for service-to-service calls
  - Issues JWT access tokens to todo-springboot
  - Exposes JWKS for todos-list validation

- MySQL
  - Shared database for todo data (todos-list)

### Request flow summary

1. User logs in or registers in Flutter.
2. Flutter calls backend /auth/kratos/* endpoints (proxy to Kratos).
3. Backend receives the Kratos session token and stores it in the client (Flutter).
4. Flutter calls backend /tasks with X-Session-Token.
5. Backend validates session with Kratos /sessions/whoami and builds the Spring Security context.
6. Backend requests a Hydra access token (client credentials) and calls todos-list via Feign.
7. todos-list validates the JWT using Hydra JWKS and returns the tasks.

## Authentication and authorization

### Kratos flow (end-user)

The backend validates user sessions through Kratos so the frontend never talks to Kratos directly in production.

- Login/register endpoints (backend):
  - POST /auth/kratos/login
  - POST /auth/kratos/register
- Session validation (backend):
  - Kratos /sessions/whoami
- Roles:
  - Stored in Kratos identity traits (role: admin|user)
  - Mapped to ROLE_ADMIN or ROLE_USER in the backend filter

Flutter uses X-Session-Token headers for authenticated requests.

### Hydra flow (service-to-service)

todo-springboot uses OAuth2 client credentials to get an access token from Hydra and calls todos-list with Authorization: Bearer <token>.

- Token request: POST http://hydra:4444/oauth2/token
- JWKS endpoint (todos-list uses this): http://hydra:4444/.well-known/jwks.json

Hydra client setup (example):

```sh
curl -i -X POST http://localhost:4445/admin/clients \
  -H "Content-Type: application/json" \
  -d '{
    "client_id": "todos-service",
    "client_secret": "change-me",
    "grant_types": ["client_credentials"],
    "response_types": ["token"],
    "token_endpoint_auth_method": "client_secret_basic",
    "scope": "todos.read",
    "audience": ["todos-list"]
  }'
```

## OpenFeign integration

todo-springboot uses OpenFeign to call todos-list. A Feign RequestInterceptor adds the Hydra Bearer token for each request.

- Token acquisition: HydraTokenService
- Feign interceptor: TodosListClientConfig
- todos-list base URL: TODOSLIST_BASE_URL

## Service ports

- Frontend: http://localhost:8081
- Backend: http://localhost:9191
- todos-list: http://localhost:8082
- Hydra public: http://localhost:4444
- Hydra admin: http://localhost:4445
- Kratos public: http://localhost:4433
- Kratos admin: http://localhost:4434
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

## Configuration keys (Docker Compose)

These are the main environment variables used in the stack:

- todo-springboot
  - PERMIFY_BASE_URL
  - PERMIFY_TENANT_ID
  - TODOSLIST_BASE_URL
  - kratos.public-base-url
  - hydra.public-base-url
  - hydra.client-id
  - hydra.client-secret
  - hydra.scope
  - hydra.audience

- todos-list
  - SPRING_DATASOURCE_URL
  - SPRING_DATASOURCE_USERNAME
  - SPRING_DATASOURCE_PASSWORD
  - spring.security.oauth2.resourceserver.jwt.jwk-set-uri

## Get started (Docker Compose)

1. Build and run the stack:
   ```sh
   docker compose up --build
   ```
2. Open the app:
   - Frontend: http://localhost:8081
   - Backend: http://localhost:9191
3. Stop the stack:
   ```sh
   docker compose down
   ```

## Get started (Kubernetes with Minikube)

1. Start Minikube:
   ```sh
   minikube start
   ```
2. Use Minikube Docker daemon (so images are visible inside the cluster):
   ```sh
   eval $(minikube docker-env)
   ```
3. Build images:
   ```sh
   docker build -t todo-springboot:latest ./todo-springboot
   docker build -t todos-list:latest ./todos-list
   docker build -t todo-flutter-web:latest ./todo_flutter
   ```
4. Apply manifests:
   ```sh
   kubectl apply -f kubernetes/
   ```
5. Access the frontend:
   ```sh
   minikube service todo-frontend
   ```

## Observability (Prometheus + Grafana)

### Install on Kubernetes using Helm

1. Add the Helm repo:
   ```sh
   helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
   helm repo update
   ```
2. Create the namespace:
   ```sh
   kubectl create namespace monitoring
   ```
3. Install kube-prometheus-stack:
   ```sh
   helm install monitoring prometheus-community/kube-prometheus-stack -n monitoring
   ```
4. Port-forward Grafana:
   ```sh
   kubectl -n monitoring port-forward svc/monitoring-grafana 3000:80
   ```
5. Open Grafana:
   - http://localhost:3000
   - Default user: admin
   - Default password:
     ```sh
     kubectl -n monitoring get secret monitoring-grafana -o jsonpath="{.data.admin-password}" | base64 --decode
     ```
6. Port-forward Prometheus:
   ```sh
   kubectl -n monitoring port-forward svc/monitoring-kube-prometheus-prometheus 9090:9090
   ```
7. Open Prometheus:
   - http://localhost:9090

### Uninstall

```sh
helm uninstall monitoring -n monitoring
```

## Notes

- The backend expects MySQL at service name "mysql" in Kubernetes.
- If you use Docker Compose only, MySQL is included in docker-compose.yml.
