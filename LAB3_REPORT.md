# Лабораторна робота №3: Розгортання власного застосунку в Kubernetes

**Дисципліна:** Технології DevOps  
**Студент:** Короленко Влад Романович (`vlkorolenko`)  
**Проєкт:** `DevOps TechStore` (`product-service`, `order-service`, `frontend`, `postgres`)  
**Кластер:** Локальний кластер **Minikube v1.39.0** (`driver=docker`, Kubernetes `v1.37.0`)  
**Простір імен:** `devops-techstore`  
**Звіти зі скріншотами:** [`LAB3_REPORT.pdf`](LAB3_REPORT.pdf) · [`LAB3_REPORT.docx`](LAB3_REPORT.docx) · [`docs/screenshots/`](docs/screenshots/)

---

## 📋 Зміст (відповідність кожному пункту і підпункту завдання)

| Пункт | Підпункти | Стан |
|------|-----------|------|
| **1. Підготувати образ застосунку** | `1a` Dockerfiles · `1b` GHCR + Minikube · `1c` Конкретні теги без `latest` · `1d` Дві версії (`1.0.0`, `1.1.0`) | ✅ Виконано |
| **2. Розгорнути локальний кластер** | `2a` `minikube` & `kubectl` · `2b` `driver=docker` · `2c` Node & `kube-system` · `2d` `metrics-server` & `ingress` | ✅ Виконано |
| **3. Окремий namespace** | `3a` `devops-techstore` · `3b` Ізоляція всіх об'єктів у цьому namespace | ✅ Виконано |
| **4. Описати маніфести (`k8s/`)** | `4a` 1 файл = 1 об'єкт · `4b` Deployment · `4c` Service · `4d` ConfigMap · `4e` Secret · `4f` Ingress · `4g` PVC | ✅ Виконано |
| **5. Розгорнути застосунок** | `5a` 1 команда `kubectl apply` · `5b` Об'єкти + метрики · `5c` Браузер UI + БД · `5d` ConfigMap/Secret у подах | ✅ Виконано |
| **6. Керування життєвим циклом** | `6a` Self-healing · `6b` Масштабування (4) + балансування · `6c` Rolling Update · `6d` History & Rollback · `6e` Без простою | ✅ Виконано |
| **7. Виконати діагностику** | `7a` `ImagePullBackOff` · `7b` `describe` Events · `7c` `logs` & `exec -it` · `7d` Виправлення та відновлення | ✅ Виконано |
| **8. Оформити результати** | `8a` Коміти `k8s/` у Git · `8b` `README.md` (порядок + ConfigMap/Secret) · `8c` Звіт зі скріншотами | ✅ Виконано |

---

## 1. Підготувати образ застосунку

### 1a) Використати образ власного проєкту, зібраний у попередніх лабораторних
Використано власні `Dockerfile` мікросервісів `product-service`, `order-service` та `frontend` із Лабораторних №1–2 із підтримкою аргументу збірки `APP_VERSION`.

![Dockerfiles та сервіси проєкту DevOps TechStore](docs/screenshots/1a-project-images.png)

### 1b) Образ має бути опублікований у registry (GHCR або локально завантажений у minikube)
Образи зібрані CI/CD пайплайном [`.github/workflows/ci.yml`](.github/workflows/ci.yml) (multi-arch `linux/amd64`, `linux/arm64`), опубліковані в **GHCR** (`ghcr.io/vlkorolenko/devops-lab1/*`) та завантажені в **Minikube**.

![Опубліковані контейнерні пакети у GHCR](docs/screenshots/1b-ghcr-packages.png)

![Успішні CI/CD збірки у GitHub Actions](docs/screenshots/1b-github-actions-ci.png)

### 1c) Образ позначити конкретним тегом версії, тег `latest` не використовувати
У всіх YAML-маніфестах каталогу [`k8s/`](k8s/) вказані конкретні версії (`:1.0.0`, `postgres:16-alpine`), тег `:latest` не використовується.

![Конкретні теги версій у k8s/ без latest](docs/screenshots/1c-no-latest-tag.png)

### 1d) Підготувати щонайменше дві версії образу (`1.0.0` і `1.1.0`)
Підготовлено версії `1.0.0` та `1.1.0` (Git-теги `v1.0.0` і `v1.1.0`, образи в GHCR і локально в Minikube).

![Дві версії образів 1.0.0 і 1.1.0](docs/screenshots/1d-two-versions.png)

---

## 2. Розгорнути локальний кластер

### 2a) Встановити `minikube` та `kubectl`
Встановлено утиліти `minikube v1.39.0` та `kubectl v1.37.2`.

![Встановлені minikube та kubectl](docs/screenshots/2a-minikube-kubectl.png)

### 2b) Запустити кластер з драйвером `docker`
Кластер `minikube` (`v1.37.0`) працює на драйвері `docker` (`containerd`).

![Кластер Minikube з драйвером docker](docs/screenshots/2b-docker-driver.png)

### 2c) Перевірити стан вузла та системних подів у namespace `kube-system`
Нода `minikube` у статусі `Ready`, усі поди `kube-system` у статусі `Running`.

![Стан вузла та системних подів kube-system](docs/screenshots/2c-nodes-kube-system.png)

### 2d) Увімкнути доповнення `metrics-server` та `ingress`
Обидва доповнення активовані, контролер `ingress-nginx` працює, `kubectl top nodes` повертає метрики.

![Аддони metrics-server та ingress](docs/screenshots/2d-addons-metrics-ingress.png)

---

## 3. Створити окремий namespace для проєкту

### 3a) Namespace назвати за іменем проєкту або прізвищем студента
У файлі [`k8s/00-namespace.yaml`](k8s/00-namespace.yaml) визначено окремий простір імен `devops-techstore`.

![Створення Namespace devops-techstore](docs/screenshots/3a-namespace-created.png)

### 3b) Усі подальші об'єкти створювати лише в цьому namespace
У кожному маніфесті `k8s/` прописано `namespace: devops-techstore`, і всі об'єкти розгортаються виключно в ньому.

![Усі об'єкти застосунку в namespace devops-techstore](docs/screenshots/3b-all-objects-in-ns.png)

---

## 4. Описати маніфести застосунку

### 4a) Усі маніфести зберігати в каталозі `k8s/` у репозиторії проєкту, по одному файлу на об'єкт
У каталозі [`k8s/`](k8s/) створено 13 ручних YAML-маніфестів за правилом «один файл — один об'єкт».

![Каталог k8s/: один файл — один об'єкт](docs/screenshots/4a-k8s-one-file-one-object.png)

### 4b) `Deployment`: ≥ 2 репліки, узгоджені `labels` і `selector`, `requests/limits`, `liveness`/`readiness`-проби
Сервіси `product-service`, `order-service`, `frontend` описані з `replicas: 2`, узгодженими `matchLabels` та `labels`, ресурсами `requests/limits` і HTTP-пробами.

![Deployment специфікація: replicas, labels/selector, limits, probes](docs/screenshots/4b-deployment-spec.png)

### 4c) `Service`: тип для доступу всередині кластера та з хоста
Внутрішні мікросервіси та БД мають тип `ClusterIP`, а `frontend` — `NodePort` (`80:30080/TCP`).

![Маніфести Service: ClusterIP і NodePort](docs/screenshots/4c-services-spec.png)

### 4d) `ConfigMap`: щонайменше два параметри конфігурації (змінні оточення або файл)
У [`k8s/01-configmap.yaml`](k8s/01-configmap.yaml) налаштовано 7 змінних оточення (`configMapKeyRef`) та файл `store-config.json` (монтується через `volumeMounts` у `frontend`).

![Маніфест ConfigMap та монтування у frontend](docs/screenshots/4d-configmap-spec.png)

### 4e) `Secret`: щонайменше один чутливий параметр
У [`k8s/02-secret.yaml`](k8s/02-secret.yaml) задано тестові паролі БД та ключ `INTERNAL_API_KEY`, підключені через `secretKeyRef`.

![Маніфест Secret та підключення через secretKeyRef](docs/screenshots/4e-secret-spec.png)

### 4f) `Ingress` для зовнішнього HTTP-доступу
У [`k8s/12-ingress.yaml`](k8s/12-ingress.yaml) описано `Ingress` класу `nginx`, що направляє запити `/` на сервіс `frontend`.

![Маніфест Ingress](docs/screenshots/4f-ingress-spec.png)

### 4g) `PersistentVolumeClaim` для компоненту зі станом (`postgres`)
У [`k8s/03-postgres-pvc.yaml`](k8s/03-postgres-pvc.yaml) створено том `postgres-pvc` (`1Gi`, `ReadWriteOnce`), підключений у [`k8s/04-postgres-deployment.yaml`](k8s/04-postgres-deployment.yaml).

![Маніфест PVC та статус Bound](docs/screenshots/4g-pvc-spec.png)

---

## 5. Розгорнути застосунок

### 5a) Застосувати всі маніфести однією командою з каталогу
Весь застосунок розгорнуто з нуля однією командою `kubectl apply -f k8s/`.

![Розгортання однією командою kubectl apply -f k8s/](docs/screenshots/5a-apply-single-cmd.png)

### 5b) Показати перелік створених об'єктів: поди, `Deployment`, `ReplicaSet`, `Service`
Усі 7 подів, 4 Deployments, 4 ReplicaSets, 4 Services та метрики `kubectl top pods` у справному стані.

![Перелік створених об'єктів і метрик](docs/screenshots/5b-created-objects.png)

### 5c) Відкрити застосунок у браузері та показати, що він працює
Застосунок відкрито в браузері Google Chrome через Ingress: завантажуються 6 товарів із PostgreSQL, банер із `ConfigMap`, ім'я пода та оформлення замовлення #1.

![Вебінтерфейс DevOps TechStore v1.0.0 у браузері](docs/screenshots/5c-browser-techstore-v1.png)

![Модальне вікно Історія замовлень у браузері](docs/screenshots/5c-browser-orders-modal.png)

### 5d) Показати, що значення з `ConfigMap` і `Secret` справді потрапили в контейнер
Перевірено змінні оточення через `kubectl exec ... -- env`, вміст `/usr/share/nginx/html/config/store-config.json` та `/actuator/product-service/info`.

![Значення ConfigMap і Secret усередині контейнерів](docs/screenshots/5d-configmap-secret-exec.png)

---

## 6. Продемонструвати керування життєвим циклом

### 6a) Самовідновлення: видалити под і показати, що кластер створив новий
Після видалення одного з подів `frontend` контролер `ReplicaSet` миттєво створив новий под (`2/2 Running`).

![Самовідновлення після видалення пода](docs/screenshots/6a-self-healing.png)

### 6b) Масштабування: збільшити кількість реплік і показати розподіл трафіку між ними
Масштабування `frontend` з 2 до 4 реплік та розподіл 20 запитів між усіма 4 подами на `NodePort` та `Ingress`.

![Масштабування та балансування трафіку на 4 поди](docs/screenshots/6b-scaling-load-balancing.png)

### 6c) Оновлення: замінити образ на другу версію (`1.1.0`) та показати процес `rolling update`
Оновлення `frontend` та `product-service` з `1.0.0` на `1.1.0` зі стратегією `RollingUpdate` і перевірка нової версії в API та браузері.

![Rolling Update до версії 1.1.0](docs/screenshots/6c-rolling-update.png)

![Вітрина v1.1.0 у браузері після Rolling Update](docs/screenshots/6c-browser-techstore-v1-1-0.png)

### 6d) Історія оновлень та відкат до попередньої ревізії
Перевірка історії (`kubectl rollout history`) та повернення попередньої версії `1.0.0` командою `kubectl rollout undo`.

![Історія оновлень та Rollback до 1.0.0](docs/screenshots/6d-history-rollback.png)

### 6e) Показати, що під час оновлення застосунок залишався доступним
Паралельний HTTP-зонд зафіксував `33/33 HTTP 200 OK` (`Failures=0`) при переході з `1.0.0` на `1.1.0`.

![Безперервна доступність під час Rolling Update](docs/screenshots/6e-zero-downtime.png)

---

## 7. Виконати діагностику

### 7a) Навмисно вказати неіснуючий тег образу і показати стан `ImagePullBackOff`
При встановленні тегу `frontend:9.9.9-nonexistent` новий под переходить у статус `ImagePullBackOff`, водночас старі репліки обслуговують запити.

![Симуляція помилки ImagePullBackOff](docs/screenshots/7a-imagepullbackoff.png)

### 7b) Знайти причину за допомогою опису пода та подій (`Events`)
Через `kubectl describe pod` у секції `Events` знайдено точну помилку (`not found`, `ErrImagePull`, `ImagePullBackOff`).

![Діагностика Events у kubectl describe pod](docs/screenshots/7b-describe-pod-events.png)

### 7c) Показати логи контейнера та вхід у контейнер в інтерактивному режимі
Перевірено логи бекенду (`kubectl logs`) та інтерактивний вхід у контейнер `frontend` (`kubectl exec -it ... -- sh`).

![Логи контейнера та інтерактивний kubectl exec -it](docs/screenshots/7c-logs-and-exec.png)

### 7d) Виправити помилку і довести застосунок до робочого стану
Виправлено тег на `:1.0.0`; усі 7 подів і 4 Deployments успішно повернулись у стан `Running` (`RESTARTS: 0`).

![Виправлення тегу та відновлення кластера](docs/screenshots/7d-recovery.png)

---

## 8. Оформити результати

### 8a) Каталог `k8s/` з маніфестами закомітити в репозиторій проєкту
Усі 13 маніфестів [`k8s/`](k8s/) закомічено в гілку `main` репозиторію `vlkorolenko/devops-lab1`.

![Коміти Git та файли каталогу k8s/](docs/screenshots/8a-git-committed.png)

### 8b) У `README` описати порядок розгортання та перелік параметрів у `ConfigMap` і `Secret`
У [`README.md`](README.md) наведено таблицю параметрів `ConfigMap`/`Secret` та покрокові команди розгортання з нуля.

![Документація в README.md](docs/screenshots/8b-readme-docs.png)

### 8c) Підготувати короткий звіт зі скріншотами або збереженим виводом команд
Підготовлено деталізований звіт по кожному пункту й підпункту у форматах [`LAB3_REPORT.md`](LAB3_REPORT.md), [`LAB3_REPORT.docx`](LAB3_REPORT.docx) та [`LAB3_REPORT.pdf`](LAB3_REPORT.pdf).

![Готові файли звіту та скріншоти](docs/screenshots/8c-report-deliverables.png)
