# Лабораторна робота №1: Віртуалізація та контейнеризація
**Предмет:** Технології DevOps  
**Проєкт:** DevOps TechStore (Мікросервісний інтернет-магазин на Java & Spring Boot)

---

## 📋 Огляд проєкту та архітектура

**DevOps TechStore** — це повнофункціональний веб-додаток електронної комерції, побудований за сучасною мікросервісною архітектурою на базі **Java 21**, **Spring Boot 3**, **PostgreSQL 16**, **Nginx** та **Docker Compose**.

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

## ✅ Покриття вимог лабораторної роботи

| № | Вимога завдання | Як реалізовано в проєкті |
|---|----------------|--------------------------|
| **1a** | Будь-яка мова та технології | **Java 21**, **Spring Boot 3.3**, Maven, Nginx |
| **1b** | Веб-додаток + база даних | REST API сервіси підключені до **PostgreSQL 16** (з автоматичною міграцією та seed-даними) |
| **1c** | Юніт або інтеграційні тести | **18 тестів** (JUnit 5, Mockito, MockMvc, Spring Boot Test, H2 In-Memory) |
| **1d** | Складається з кількох частин (мікросервіси) | 3 незалежні сервіси (`product-service`, `order-service`, `frontend`) + PostgreSQL |
| **2** | Dockerfile для кожного сервісу | Multi-stage Dockerfiles для обох Spring Boot додатків + оптимізований Nginx Dockerfile |
| **3** | Docker Compose файл | `docker-compose.yml` з декларацією сервісів, healthcheck, volume persistence та мережею |
| **4** | Збереження у Git / GitHub | Ініціалізовано git, підготовлено `.gitignore` та `.github/workflows/ci.yml` |

---

## 🚀 Швидкий запуск проєкту

### 1. Запуск через Docker Compose (Всі сервіси)

Для збірки та старту всіх контейнерів виконайте команду:

```bash
docker compose up -d --build
```
*або через Makefile:*
```bash
make up
```

Після запуску всі сервіси піднімуться у правильному порядку з урахуванням `healthcheck` бази даних:
- **Веб-магазин (Frontend UI):** [http://localhost:8080](http://localhost:8080)
- **Product Service API:** [http://localhost:8081/api/products](http://localhost:8081/api/products)
- **Order Service API:** [http://localhost:8082/api/orders](http://localhost:8082/api/orders)
- **База даних PostgreSQL:** `localhost:5432` (`shop_db`, user: `shop_user`)

---

## 🧪 Запуск тестів (Unit & Integration)

Всі тести ізольовані та виконуються у контейнері з Maven та базою даних H2 в пам'яті (In-Memory), що не потребує встановленої локально Java:

```bash
./run_tests.sh
```
*або через Makefile:*
```bash
make test
```

### Тестове покриття:
1. **Product Service (11 тестів):**
   - `ProductServiceTest` (Unit): отримання списку, фільтрація за категорією, валідація, створення товару, обробка відсутності ID.
   - `ProductControllerTest` (Integration): тестування REST ендпоінтів `GET /api/products`, `GET /api/products/categories`, перевірка валідації `400 Bad Request`, `404 Not Found`, `GET /actuator/health`.
2. **Order Service (7 тестів):**
   - `OrderServiceTest` (Unit): розрахунок вартості замовлення (`sum(price * quantity)`), створення замовлення, отримання за ID.
   - `OrderControllerTest` (Integration): `GET /api/orders`, створення замовлення `POST /api/orders` (201 Created), валідація пустого кошика `400 Bad Request`, `GET /actuator/health`.

---

## 📡 Огляд REST API ендпоінтів

### Product Service (`:8081` або через фронтенд `:8080/api/products`):
- `GET /api/products` — отримати список усіх товарів (підтримує `?category=...` та `?search=...`)
- `GET /api/products/categories` — отримати перелік унікальних категорій
- `GET /api/products/{id}` — отримати товар за ID
- `POST /api/products` — додати новий товар
- `DELETE /api/products/{id}` — видалити товар
- `GET /actuator/health` — статус працездатності (Healthcheck)

### Order Service (`:8082` або через фронтенд `:8080/api/orders`):
- `GET /api/orders` — перелік замовлень
- `GET /api/orders/{id}` — деталі замовлення
- `POST /api/orders` — оформити нове замовлення
- `GET /actuator/health` — статус працездатності (Healthcheck)

#### Приклад створення замовлення через `curl`:
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Олександр Шевченко",
    "customerEmail": "oleksandr@example.com",
    "customerAddress": "м. Київ, вул. Хрещатик 1",
    "items": [
      {
        "productId": 1,
        "productName": "Ноутбук Pro Developer 16\"",
        "price": 1899.99,
        "quantity": 1
      }
    ]
  }'
```

---

## 🛠 Корисні команди управління

| Команда | Опис |
|---------|------|
| `make up` або `docker compose up -d` | Запустити всі контейнери у фоновому режимі |
| `make down` або `docker compose down` | Зупинити роботу контейнерів |
| `make logs` або `docker compose logs -f` | Переглянути поточні логи всіх контейнерів |
| `make test` або `./run_tests.sh` | Запустити всі 18 тестів |
| `make ps` або `docker compose ps` | Перевірити статус та healthcheck сервісів |
| `make clean` | Зупинити контейнери та видалити створені томи бази даних |

---

## 📤 Завантаження проєкту на GitHub (Вимога 4)

1. Створіть новий порожній репозиторій на [GitHub](https://github.com/new) (наприклад, `devops-lab1`).
2. Виконайте в терміналі команди:

```bash
git remote add origin https://github.com/<YOUR_USERNAME>/devops-lab1.git
git branch -M main
git push -u origin main
```
