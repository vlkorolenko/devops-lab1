# DevOps TechStore — Лабораторні роботи №1 та №2
**Предмет:** Технології DevOps  
**Теми:** «Віртуалізація та контейнеризація» (Лаб 1) та «Continuous Integration & Delivery» (Лаб 2)

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
