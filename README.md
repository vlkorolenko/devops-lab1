# DevOps TechStore — Лабораторні роботи №1, №2 та №3
**Предмет:** Технології DevOps  
**Теми:** «Віртуалізація та контейнеризація» (Лаб 1), «Continuous Integration & Delivery» (Лаб 2) та «Розгортання власного застосунку в Kubernetes» (Лаб 3)

---

## 📋 Огляд проєкту та архітектура

**DevOps TechStore** — це повнофункціональний веб-додаток електронної комерції, побудований за мікросервісною архітектурою на базі **Java 21**, **Spring Boot 3.3**, **PostgreSQL 16**, **Nginx** та **Docker Compose**, з автоматизованим CI/CD пайплайном у **GitHub Actions** та публікацією образів у **GitHub Container Registry (GHCR)**.

```
                           +---------------------------+
                           |  Користувач / Браузер     |
                           +-------------+-------------+
                                         |
                                         | HTTP :8080
                                         v
               +-------------------------------------------------------+
               |            Frontend (Nginx Reverse Proxy)             |
               |  - Статичний SPA інтерфейс (HTML5, TailwindCSS, JS)   |
               |  - /api/products -> проксі до product-service:8081    |
               |  - /api/orders   -> проксі до order-service:8082      |
               +-------------------+-------------------+---------------+
                                   |                   |
                     HTTP :8081    |                   |  HTTP :8082
             +---------------------+                   +---------------------+
             v                                                               v
+--------------------------+                               +--------------------------+
|     product-service      |                               |      order-service       |
|  - Spring Boot 3.3       |                               |  - Spring Boot 3.3       |
|  - Java 21               |                               |  - Java 21               |
|  - REST API Каталогу     |                               |  - REST API Замовлень    |
|  - Spring Data JPA       |                               |  - Spring Data JPA       |
|  - Actuator Healthcheck  |                               |  - Actuator Healthcheck  |
+------------+-------------+                               +------------+-------------+
             |                                                          |
             | JDBC :5432                                               | JDBC :5432
             +---------------------+              +---------------------+
                                   v              v
                        +------------------------------------+
                        |       PostgreSQL 16 Database       |
                        |      (shop_db / postgres_data)     |
                        |   - products                       |
                        |   - orders & order_items           |
                        +------------------------------------+
```

---

## 🔄 Лабораторна робота №2: Схема CI/CD Пайплайну (GitHub Actions)

Пайплайн налаштовано у файлі [`.github/workflows/ci.yml`](.github/workflows/ci.yml):

```
+-----------------------------------------------------------------------------------+
|                            Тригер: Push або Pull Request                          |
+-----------------------------------------+-----------------------------------------+
                                          |
        +---------------------------------+---------------------------------+
        v                                                                   v
+---------------------------------------+                 +-----------------------------------+
|  Job 1: backend-ci (matrix: 2 jobs)   |                 |       Job 2: frontend-ci          |
|  [product-service, order-service]     |                 |  1. Validate Nginx config (test)  |
|  1. Checkstyle (Static Analysis)      |                 |  2. Verify Static Assets (HTML/JS)|
|  2. Build (compile & package)         |                 +-----------------+-----------------+
|  3. Test (JUnit 5 & MockMvc)          |                                   |
|  * fail-fast: false (паралельно)      |                                   |
+-------------------+-------------------+                                   |
                    |                                                       |
                    +-----------------------------+-------------------------+
                                                  |
                                                  v (needs: [backend-ci, frontend-ci])
                        +---------------------------------------------------+
                        | Job 3: docker-build-and-push (matrix: 3 services) |
                        | [product-service, order-service, frontend]        |
                        | 1. Docker Buildx setup                            |
                        | 2. Build Docker images from Dockerfiles           |
                        | 3. Security Scan with Trivy (Vulnerabilities)     |
                        | 4. Tagging: sha-<commit> та latest                |
                        | 5. Push to ghcr.io (ТІЛЬКИ при push у main)       |
                        +---------------------------------------------------+
```

---

## ✅ Відповідність усім вимогам Лабораторної №2

| № | Вимога завдання | Як реалізовано в проєкті |
|---|----------------|--------------------------|
| **1** | Підключити CI/CD (GitHub Actions) | Створено `.github/workflows/ci.yml`. Запускається на `push` у `main` та на будь-який `pull_request`. Статуси відображаються у PR. |
| **2** | Збірка та тестування | Окремі матричні завдання з незалежним виконанням (`fail-fast: false`). Кроки `Build` (`mvn compile`) та `Test` (`mvn test`). Кешування через `cache: maven`. |
| **3** | Статичний аналіз коду (Лінтер) | Підключено **Checkstyle** (`maven-checkstyle-plugin`) з `checkstyle.xml`. Крок є **блокуючим**: порушення правил негайно зупиняють пайплайн з помилкою. |
| **4** | Збірка контейнерних образів | Завдання `docker-build-and-push` залежить (`needs`) від успіху тестів та лінтера. Додано безпекове сканування через **Trivy** (`aquasecurity/trivy-action`). |
| **5** | Публікація в реєстрі (GHCR) | Автентифікація через `GITHUB_TOKEN`. Публікація в `ghcr.io` тільки для `push` у `main` (у PR лише збірка без пушу). Теги: `sha-${{ github.sha }}` та `latest`. |
| **6** | Захист основної гілки | Інструкція з налаштування Branch Protection Rules на GitHub. Створено сценарій демонстрації заблокованого PR при зламаному тесті. |

---

## 🚀 Швидкий локальний запуск проєкту

### 1. Запуск через Docker Compose (Всі 4 сервіси)
```bash
docker compose up -d --build
```
*або через Makefile:*
```bash
make up
```

- **Веб-магазин (Frontend UI):** [http://localhost:8080](http://localhost:8080)
- **Product Service API:** [http://localhost:8081/api/products](http://localhost:8081/api/products)
- **Order Service API:** [http://localhost:8082/api/orders](http://localhost:8082/api/orders)

### 2. Запуск локального лінтера та тестів
```bash
./run_tests.sh
```
*або через Makefile:*
```bash
make test
```
Скрипт перевірить Checkstyle та виконає 18 тестів для обох сервісів.

---

## 🛡️ Налаштування захисту гілки (Branch Protection) на GitHub

Щоб виконати вимогу №6:

1. Відкрийте ваш репозиторій на GitHub: [https://github.com/vlkorolenko/devops-lab1](https://github.com/vlkorolenko/devops-lab1)
2. Перейдіть у **Settings** ➡️ зліва виберіть **Branches**.
3. У блоці **Branch protection rules** натисніть кнопку **Add branch ruleset** або **Add rule**.
4. В полі **Branch name pattern** вкажіть: `main`.
5. Увімкніть галочки:
   - ✅ **Require a pull request before merging** (вимагати PR перед злиттям, блокує прямий push).
   - ✅ **Require status checks to pass before merging** (вимагати успішного проходження перевірок).
   - У пошуку перевірок виберіть: `Backend CI (product-service)`, `Backend CI (order-service)`, `Frontend CI (Validation)`.
6. Натисніть **Save changes** (або **Create**).

---

## 💥 Демонстрація заблокованого Pull Request (Вимога 6)

Щоб продемонструвати викладачу, що захист гілки працює і блокує злиття:

1. Створіть нову гілку для перевірки:
   ```bash
   git checkout -b feature/broken-test
   ```
2. Зламайте будь-який тест (наприклад, у `product-service/src/test/java/com/devops/shop/product/ProductServiceTest.java` змініть очікуваний розмір з `1` на `99`).
3. Запушіть гілку:
   ```bash
   git add .
   git commit -m "test: simulate broken test for PR check"
   git push -u origin feature/broken-test
   ```
4. Відкрийте Pull Request на GitHub.
5. GitHub Actions автоматично запуститься, впаде з помилкою ❌, і кнопка **Merge pull request** буде заблокована!

---

## 📦 Перевірка опублікованих образів у GHCR (Вимога 5)

Після кожного push у гілку `main` образи публікуються в реєстр GitHub Packages:
- `ghcr.io/vlkorolenko/devops-lab1/product-service:latest`
- `ghcr.io/vlkorolenko/devops-lab1/order-service:latest`
- `ghcr.io/vlkorolenko/devops-lab1/frontend:latest`

Їх можна стягнути командою:
```bash
docker pull ghcr.io/vlkorolenko/devops-lab1/product-service:latest
```

---

## ☸️ Лабораторна робота №3: Розгортання в Kubernetes (Minikube)

Проєкт повністю перенесено на маніфести Kubernetes у каталозі [`k8s/`](k8s/) (один файл — один об'єкт) з окремим namespace **`devops-techstore`** і версіонованими образами **`1.0.0`** та **`1.1.0`** (без використання тегу `latest`).

### 📂 Структура каталогу `k8s/`

| Файл | Об'єкт Kubernetes (`kind`) | Призначення |
|------|---------------------------|-------------|
| `k8s/00-namespace.yaml` | `Namespace` | Ізольований простір імен `devops-techstore` |
| `k8s/01-configmap.yaml` | `ConfigMap` | Нечутливі параметри конфігурації (`techstore-config`) + JSON-файл конфігурації |
| `k8s/02-secret.yaml` | `Secret` | Демо-облікові дані БД та API-ключ (`techstore-secret`) |
| `k8s/03-postgres-pvc.yaml` | `PersistentVolumeClaim` | Постійний том `1Gi` (`ReadWriteOnce`) для PostgreSQL |
| `k8s/04-postgres-deployment.yaml` | `Deployment` | База даних PostgreSQL 16 (`postgres:16-alpine`, `Recreate`, проби + PVC) |
| `k8s/05-postgres-service.yaml` | `Service` | Внутрішній сервіс `ClusterIP` для PostgreSQL (`:5432`) |
| `k8s/06-product-service-deployment.yaml` | `Deployment` | Каталог товарів (`2` репліки, `:1.0.0`, CPU/RAM limits, `/actuator/health/*`) |
| `k8s/07-product-service-service.yaml` | `Service` | Внутрішній сервіс `ClusterIP` (`:8081`) |
| `k8s/08-order-service-deployment.yaml` | `Deployment` | Сервіс замовлень (`2` репліки, `:1.0.0`, CPU/RAM limits, `/actuator/health/*`) |
| `k8s/09-order-service-service.yaml` | `Service` | Внутрішній сервіс `ClusterIP` (`:8082`) |
| `k8s/10-frontend-deployment.yaml` | `Deployment` | Nginx + Web UI (`2` репліки, `:1.0.0`, монтування `store-config.json` + проби) |
| `k8s/11-frontend-service.yaml` | `Service` | Сервіс типу `NodePort` (`:80` -> `30080`) для доступу всередині кластера і з хоста |
| `k8s/12-ingress.yaml` | `Ingress` | Вхідна маршрутизація HTTP через `ingress-nginx` на сервіс `frontend` |

---

### 🔧 Перелік параметрів у `ConfigMap` (`techstore-config`) та `Secret` (`techstore-secret`)

#### 1. `ConfigMap` (`k8s/01-configmap.yaml`)

| Ключ | Значення | Куди та як підключено |
|------|----------|-----------------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/shop_db` | Змінна оточення (`configMapKeyRef`) у `product-service`, `order-service` |
| `APP_ENVIRONMENT` | `kubernetes-minikube` | Змінна оточення у `product-service`, `order-service`, `frontend` |
| `APP_STORE_NAME` | `DevOps TechStore (Kubernetes)` | Змінна оточення у `product-service`, `order-service`, `frontend` |
| `SPRING_JPA_SHOW_SQL` | `false` | Змінна оточення у `product-service`, `order-service` |
| `LOGGING_LEVEL_COM_DEVOPS_SHOP` | `INFO` | Змінна оточення у `product-service`, `order-service` |
| `PRODUCT_SERVICE_HOST` | `product-service.devops-techstore.svc.cluster.local` | FQDN-адреса бекенд-сервісу каталогу для Nginx reverse proxy у `frontend` |
| `ORDER_SERVICE_HOST` | `order-service.devops-techstore.svc.cluster.local` | FQDN-адреса бекенд-сервісу замовлень для Nginx reverse proxy у `frontend` |
| `store-config.json` | JSON-конфігурація вітрини (назва, валюта, банер) | Файл через `volumeMounts` (`subPath`) в `/usr/share/nginx/html/config/store-config.json` у `frontend` |

#### 2. `Secret` (`k8s/02-secret.yaml`, тип `Opaque`, тестові неробочі дані)

| Ключ | Призначення | Куди підключено |
|------|-------------|-----------------|
| `POSTGRES_DB` | Ім'я бази даних (`shop_db`) | `postgres` (`secretKeyRef`) |
| `POSTGRES_USER` | Демо-користувач БД | `postgres` (`secretKeyRef`) |
| `POSTGRES_PASSWORD` | Демо-пароль БД | `postgres` (`secretKeyRef`) |
| `SPRING_DATASOURCE_USERNAME` | Логін для JDBC-підключення Spring Boot | `product-service`, `order-service` (`secretKeyRef`) |
| `SPRING_DATASOURCE_PASSWORD` | Пароль для JDBC-підключення Spring Boot | `product-service`, `order-service` (`secretKeyRef`) |
| `INTERNAL_API_KEY` | Демонстраційний сервісний API-ключ | `product-service`, `order-service` (`secretKeyRef`) |

---

### 🚀 Порядок розгортання з нуля в Minikube

#### 1. Підготовка та запуск кластера
```bash
# Запустити локальний кластер із драйвером Docker
minikube start --driver=docker --cpus=4 --memory=3072mb

# Перевірити вузол та системні поди
kubectl get nodes -o wide
kubectl get pods -n kube-system

# Увімкнути доповнення metrics-server та ingress
minikube addons enable metrics-server
minikube addons enable ingress
```

#### 2. Підготовка версіонованих образів (`1.0.0` і `1.1.0`)
Образи з конкретними тегами версій публікуються у GHCR (або завантажуються напряму в Minikube):
```bash
docker build --build-arg APP_VERSION=1.0.0 -t ghcr.io/vlkorolenko/devops-lab1/product-service:1.0.0 ./product-service
docker build --build-arg APP_VERSION=1.1.0 -t ghcr.io/vlkorolenko/devops-lab1/product-service:1.1.0 ./product-service
docker build --build-arg APP_VERSION=1.0.0 -t ghcr.io/vlkorolenko/devops-lab1/order-service:1.0.0 ./order-service
docker build --build-arg APP_VERSION=1.1.0 -t ghcr.io/vlkorolenko/devops-lab1/order-service:1.1.0 ./order-service
docker build --build-arg APP_VERSION=1.0.0 -t ghcr.io/vlkorolenko/devops-lab1/frontend:1.0.0 ./frontend
docker build --build-arg APP_VERSION=1.1.0 -t ghcr.io/vlkorolenko/devops-lab1/frontend:1.1.0 ./frontend

# Завантаження локально зібраних образів у minikube
minikube image load postgres:16-alpine
minikube image load ghcr.io/vlkorolenko/devops-lab1/product-service:1.0.0
minikube image load ghcr.io/vlkorolenko/devops-lab1/product-service:1.1.0
minikube image load ghcr.io/vlkorolenko/devops-lab1/order-service:1.0.0
minikube image load ghcr.io/vlkorolenko/devops-lab1/order-service:1.1.0
minikube image load ghcr.io/vlkorolenko/devops-lab1/frontend:1.0.0
minikube image load ghcr.io/vlkorolenko/devops-lab1/frontend:1.1.0
```

#### 3. Розгортання всього застосунку однією командою
```bash
kubectl apply -f k8s/
# або через Makefile:
make k8s-up
```

#### 4. Перевірка об'єктів та відкриття застосунку
```bash
# Перегляд усіх створених об'єктів у namespace devops-techstore
kubectl get all,ingress,pvc,cm,secret -n devops-techstore

# Доступ до фронтенду на macOS (Docker driver):
# Варіант 1 (NodePort через minikube):
minikube service frontend -n devops-techstore --url

# Варіант 2 (через Ingress + minikube tunnel):
minikube tunnel
```

Детальний звіт із збереженим виводом команд для етапів розгортання, керування життєвим циклом (самовідновлення, масштабування, rolling update, rollback) і діагностики наведено у файлі [`LAB3_REPORT.md`](LAB3_REPORT.md).
