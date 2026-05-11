# Todo Spring Boot + Flutter

Monorepo with a Spring Boot backend, Flutter web frontend, and local observability (Prometheus + Grafana). Includes Docker Compose and Kubernetes manifests.

## Prerequisites

- Docker Desktop
- Java 26 (for local Spring Boot development)
- Flutter SDK (for local Flutter development)
- kubectl + minikube (for Kubernetes)
- Helm (for Prometheus + Grafana on Kubernetes)

## Project layout

- todo-springboot/ - Spring Boot backend
- todo_flutter/ - Flutter web frontend
- docker-compose.yml - Docker Compose stack
- kubernetes/ - Kubernetes manifests
- prometheus/ - Prometheus and Grafana manifests or configs

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

## Get started (Prometheus + Grafana)

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

### Uninstall

```sh
helm uninstall monitoring -n monitoring
```

## Notes

- The backend expects MySQL at service name "mysql" in Kubernetes.
- If you use Docker Compose only, MySQL is included in docker-compose.yml.
