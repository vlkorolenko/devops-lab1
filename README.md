# DevOps TechStore — Лабораторні роботи №1, №2, №3, №4 та №5
**Предмет:** Технології DevOps  
**Теми:** «Віртуалізація та контейнеризація» (Лаб 1), «Continuous Integration & Delivery» (Лаб 2), «Розгортання власного застосунку в Kubernetes» (Лаб 3), «Пакування застосунку в Helm-чарт» (Лаб 4) та «GitOps: розгортання проєкту через Argo CD» (Лаб 5)

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

Детальний звіт із збереженим виводом команд і скріншотами всіх етапів (кластер Minikube, розгортання `k8s/`, браузерні тести, самовідновлення, масштабування, Rolling Update/Rollback, діагностика та CI/CD) наведено у файлах [`LAB3_REPORT.md`](LAB3_REPORT.md), [`LAB3_REPORT.pdf`](LAB3_REPORT.pdf) та [`LAB3_REPORT.docx`](LAB3_REPORT.docx), а самі скріншоти збережено в каталозі [`docs/screenshots/`](docs/screenshots/).

---

## 📦 Лабораторна робота №4: Пакування застосунку в Helm-чарт

У межах лабораторної роботи №4 Kubernetes-маніфести з ЛР3 було повністю трансформовано у повноцінний, параметризований **Helm-чарт v2** (`chart/`), підготовлено окремі конфігураційні профілі для середовищ розробки (`values-dev.yaml`) та промислової експлуатації (`values-prod.yaml`), додано автоматичні тести релізу (`helm test`) та хуки життєвого циклу (`post-install` / `post-upgrade`).

### 1. Структура Helm-чарту (`chart/`)

```
chart/
├── Chart.yaml                  # Метадані чарту: name, version, appVersion, type, icon
├── values.yaml                 # Базові значення за замовчуванням (середовище розробки Dev)
├── values-dev.yaml             # Оверрайди для локальної розробки (1 репліка, невеликі ресурси)
├── values-prod.yaml            # Оверрайди для промислового середовища (2 репліки, Ingress, більші ресурси)
└── templates/                  # Шаблони Kubernetes-маніфестів
    ├── _helpers.tpl            # Іменовані шаблони (name, fullname, labels, selectorLabels)
    ├── NOTES.txt               # Інструкції користувачеві після розгортання релізу
    ├── configmap.yaml          # Динамічний ConfigMap (FQDN сервісів, змінні оточення, store-config.json)
    ├── secret.yaml             # Secret для паролів PostgreSQL та внутрішнього API-ключа
    ├── postgres-pvc.yaml       # PersistentVolumeClaim для PostgreSQL (з прапорцем enabled)
    ├── postgres-service.yaml   # Service для PostgreSQL (ClusterIP :5432)
    ├── postgres-deployment.yaml# Deployment PostgreSQL зі збереженням на PVC / emptyDir
    ├── product-service-service.yaml    # Service для product-service (ClusterIP :8081)
    ├── product-service-deployment.yaml # Deployment product-service (Spring Boot)
    ├── order-service-service.yaml      # Service для order-service (ClusterIP :8082)
    ├── order-service-deployment.yaml   # Deployment order-service (Spring Boot)
    ├── frontend-service.yaml   # Service для Frontend (NodePort :30080 / ClusterIP)
    ├── frontend-deployment.yaml# Deployment Frontend (Nginx SPA + Reverse Proxy)
    ├── ingress.yaml            # Ingress для зовнішнього доступу (з прапорцем enabled)
    ├── post-install-hook.yaml  # Helm Hook (Job post-install / post-upgrade валідації)
    └── tests/
        └── test-connection.yaml# Helm Test Pod для автоматичної перевірки доступності фронтенду
```

### 2. Різниця між `version` та `appVersion`

У файлі [`chart/Chart.yaml`](chart/Chart.yaml) визначено два ключових поля версіонування:
* **`version` (1.0.0)** — версія самого Helm-чарта як артефакту пакування відповідно до специфікації SemVer. Вона змінюється, коли модифікуються самі шаблони Helm (додаються нові поля в маніфести, змінюються helpers, додаються хуки чи правила валідації values), навіть якщо код мікросервісів не змінювався.
* **`appVersion` ("1.0.0")** — версія самого прикладного застосунку (**DevOps TechStore**), яка відповідає тегам контейнерних образів у реєстрі GHCR (`ghcr.io/vlkorolenko/devops-lab1/*:1.0.0`). Коли розробники випускають новий реліз коду (наприклад `1.1.0`), оновлюється `appVersion`, що дозволяє чарту за замовчуванням розгортати відповідні версії образів.

### 3. Параметри конфігурації (`values.yaml`)

| Параметр | Опис | Значення за замовчуванням (Dev) | Промислове середовище (`values-prod.yaml`) |
|---|---|---|---|
| `config.appStoreName` | Назва веб-магазину у заголовку | `DevOps TechStore (Helm Dev)` | `DevOps TechStore (Production)` |
| `config.appEnvironment` | Ідентифікатор робочого середовища | `helm-dev` | `production` |
| `config.currency` | Валюта відображення цін | `USD` | `USD` |
| `config.banner` | Текст банера в інтерфейсі SPA | `Лабораторна робота №4 • Helm Chart (Development)` | `Лабораторна робота №4 • Промислове середовище (Production)` |
| `frontend.replicaCount` | Кількість реплік frontend | `1` | `2` |
| `frontend.image.tag` | Версія Docker-образу frontend | `1.0.0` | `1.1.0` |
| `frontend.service.type` | Тип Kubernetes Service для frontend | `NodePort` | `NodePort` / `ClusterIP` |
| `frontend.service.nodePort` | Порт NodePort для прямого доступу | `30080` | `30080` |
| `frontend.resources.requests.cpu` / `memory` | Гарантовані ресурси frontend | `50m` / `32Mi` | `100m` / `64Mi` |
| `frontend.resources.limits.cpu` / `memory` | Ліміти ресурсів frontend | `150m` / `64Mi` | `250m` / `128Mi` |
| `productService.replicaCount` | Кількість реплік каталогу товарів | `1` | `2` |
| `productService.image.tag` | Версія образу product-service | `1.0.0` | `1.1.0` |
| `productService.resources.requests` | Ресурси product-service (req) | `100m` / `160Mi` | `200m` / `256Mi` |
| `productService.resources.limits` | Ресурси product-service (lim) | `300m` / `256Mi` | `600m` / `512Mi` |
| `orderService.replicaCount` | Кількість реплік сервісу замовлень | `1` | `2` |
| `orderService.image.tag` | Версія образу order-service | `1.0.0` | `1.1.0` |
| `orderService.resources.requests` | Ресурси order-service (req) | `100m` / `160Mi` | `200m` / `256Mi` |
| `orderService.resources.limits` | Ресурси order-service (lim) | `300m` / `256Mi` | `600m` / `512Mi` |
| `postgres.enabled` | Прапорець увімкнення вбудованої PostgreSQL | `true` | `true` |
| `postgres.persistence.enabled` | Прапорець постійного сховища (PVC) | `true` | `true` |
| `postgres.persistence.size` | Розмір тому PVC для PostgreSQL | `1Gi` | `2Gi` |
| `ingress.enabled` | Прапорець увімкнення Ingress | `false` | `true` |
| `ingress.className` | Клас Ingress контролера | `nginx` | `nginx` |

### 4. Інструкції встановлення та управління життєвим циклом

```bash
# 1. Перевірка чарту на помилки та лінтинг
helm lint chart/

# 2. Сухий прогін і рендеринг маніфестів
helm template techstore chart/

# 3. Встановлення релізу в середовищі розробки (Dev)
helm install techstore ./chart -n devops-techstore --create-namespace

# 4. Перегляд статусу та параметрів релізу
helm list -n devops-techstore
helm status techstore -n devops-techstore
helm get values techstore -n devops-techstore

# 5. Оновлення релізу зміною тегу frontend через аргумент CLI --set
helm upgrade techstore ./chart --set frontend.image.tag=1.1.0 -n devops-techstore

# 6. Оновлення релізу до промислового середовища (Production) через values-prod.yaml
helm upgrade techstore ./chart -f chart/values-prod.yaml -n devops-techstore

# 7. Запуск автоматичних тестів релізу (Helm Test)
helm test techstore -n devops-techstore --logs

# 8. Перегляд історії ревізій релізу
helm history techstore -n devops-techstore

# 9. Відкат релізу до першої ревізії (Rollback)
helm rollback techstore 1 -n devops-techstore

# 10. Повне видалення релізу однією командою
helm uninstall techstore -n devops-techstore
```

Детальний звіт із кожним окремим пунктом і підпунктом методички (1a–10c), письмовими поясненнями та скріншотами команд оформлено у файлах [`LAB4_REPORT.md`](LAB4_REPORT.md), [`LAB4_REPORT.pdf`](LAB4_REPORT.pdf) та [`LAB4_REPORT.docx`](LAB4_REPORT.docx), а скріншоти зібрано в [`docs/screenshots/lab4/`](docs/screenshots/lab4/).

---

## 🐙 Лабораторна робота №5: Декларативне безперервне розгортання через GitOps та Argo CD

### 1. Архітектура GitOps (App of Apps Pattern)

У лабораторній роботі №5 розгортання мікросервісного додатку **TechStore** повністю переведено на методологію **GitOps** із використанням **Argo CD v3.6.3** та Helm-чарту. Репозиторій Git виступає єдиним джерелом істини (*Single Source of Truth*), а кластер Kubernetes постійно синхронізується із бажаним станом у Git.

```
+--------------------------------------------------------------------------+
|                            GitHub Repository                             |
|                                                                          |
|  bootstrap/root.yaml  --->  argocd-apps/techstore.yaml  --->  charts/     |
|   (Root Application)         (Child Application)          (Helm Chart)   |
+------------------------------------+-------------------------------------+
                                     |
                                     | git pull / poll / webhook
                                     v
+------------------------------------+-------------------------------------+
|                     Kubernetes Cluster (Minikube)                        |
|                                                                          |
|  [argocd namespace]                                                      |
|   Argo CD Controller  <==>  argocd-server (Web UI & CLI)                 |
|            |                                                             |
|            | reconciles & applies                                        |
|            v                                                             |
|  [devops-techstore namespace]                                            |
|   - techstore-frontend        (Deploy + Svc NodePort)                    |
|   - techstore-product-service (Deploy + Svc ClusterIP)                   |
|   - techstore-order-service   (Deploy + Svc ClusterIP)                   |
|   - techstore-postgres        (Deploy + Svc + PVC)                       |
|   - techstore-secret          (Pre-created unmanaged Secret)             |
+--------------------------------------------------------------------------+
```

### 2. Структура файлів GitOps

```
├── bootstrap/
│   └── root.yaml                   # Кореневий додаток (App of Apps): відстежує argocd-apps/
├── argocd-apps/
│   └── techstore.yaml              # Дочірній додаток: відстежує charts/techstore з values-minikube.yaml
├── charts/
│   └── techstore/                  # Helm-чарт застосунку
│       ├── Chart.yaml              # Метадані чарту
│       ├── values.yaml             # Базові налаштування
│       ├── values-minikube.yaml    # Специфічні значення для Minikube (existingSecret, tag, NodePort)
│       └── templates/              # Шаблони маніфестів K8s
├── .github/workflows/
│   └── ci.yml                      # CI-пайплайн із paths-ignore для уникнення зайвих білдів
└── docs/screenshots/lab5/          # Скріншоти виконання всіх 27 підпунктів
```

### 3. Безпека секретів та оптимізація CI

- **Управління секретами (`existingSecret`):** Секрети кластера не зберігаються у відкритому вигляді в Git. У `templates/secret.yaml` реалізовано умову `{{- if not .Values.existingSecret }}`, а сам секрет `techstore-secret` створюється адміністратором безпосередньо у кластері. Argo CD керує лише деплойментами, сервісами та PVC, не чіпаючи зовнішній секрет при `prune: true`.
- **Фільтр тригерів CI (`paths-ignore`):** У `.github/workflows/ci.yml` налаштовано ігнорування шляхів `bootstrap/**`, `argocd-apps/**`, `charts/**`, `docs/**` та звітів, щоб зміни інфраструктурної конфігурації у GitOps не спричиняли повторне перевипускання Docker-образів.

### 4. Швидкий запуск та команди управління Argo CD

```bash
# 1. Встановлення Argo CD у кластер
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/v3.6.3/manifests/install.yaml

# 2. Доступ до вебінтерфейсу Argo CD (порт 8080)
kubectl -n argocd port-forward svc/argocd-server 8080:443
# Відкрити https://localhost:8080 (Логін: admin / Пароль: DevOps2026Lab5!)

# 3. Створення простору імен і секрету застосунку
kubectl create namespace devops-techstore
kubectl -n devops-techstore create secret generic techstore-secret \
  --from-literal=POSTGRES_USER=techstore_admin \
  --from-literal=POSTGRES_PASSWORD=admin_secret_pass_2026 \
  --from-literal=JWT_SECRET=super_secret_jwt_key_techstore_devops_2026_super_secure_key

# 4. Розгортання через патерн App of Apps (одна точка входу)
kubectl apply -f bootstrap/root.yaml

# 5. Перевірка статусу синхронізації та здоров'я застосунків
argocd app list
argocd app get root
argocd app get techstore

# 6. Оновлення версії застосунку через Git (Git-driven Release)
# Змінити у charts/techstore/values-minikube.yaml:
# frontend:
#   image:
#     tag: "1.1.0"
git commit -am "Release techstore 1.1.0"
git push origin main
argocd app get techstore --refresh

# 7. Перевірка оновленої версії через HTTP ендпоінт
curl http://localhost:8081/k8s-info

# 8. Тестування самовідновлення (selfHeal) при несанкціонованому дрейфі
kubectl scale deployment techstore-frontend --replicas=5 -n devops-techstore
# Argo CD автоматично повертає кількість реплік до 1 відповідно до стану в Git!

# 9. Відкат релізу у разі помилки (GitOps-way)
git revert HEAD --no-edit
git push origin main
argocd app history techstore
```

### 5. Документація та звітність

Повний детальний звіт із письмовими обґрунтуваннями всіх 7 пунктів і 27 підпунктів лабораторної роботи та скріншотами кожної консольної дії та вебінтерфейсу доступний у форматах:
- 📄 **Microsoft Word:** [`LAB5_REPORT.docx`](LAB5_REPORT.docx) (повне оформлення, 9.5 МБ)
- 📑 **PDF:** [`LAB5_REPORT.pdf`](LAB5_REPORT.pdf) (висока якість, 12 МБ)
- 📝 **Markdown:** [`LAB5_REPORT.md`](LAB5_REPORT.md)
- 🖼️ **Скріншоти:** [`docs/screenshots/lab5/`](docs/screenshots/lab5/) (30 скріншотів у високій роздільній здатності).
