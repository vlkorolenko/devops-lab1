# Лабораторна робота №3: Розгортання власного застосунку в Kubernetes

**Дисципліна:** Технології DevOps  
**Проєкт:** `DevOps TechStore` (`product-service`, `order-service`, `frontend`, `postgres`)  
**Кластер:** Локальний однонодовий кластер **Minikube v1.39.0** (драйвер `docker`, Kubernetes `v1.37.0`)  
**Простір імен:** `devops-techstore`

---

## 📋 Зміст та відповідність вимогам завдання

| Етап | Вимога | Стан виконання |
|------|--------|----------------|
| **1. Підготовка образів** | Версіоновані теги `1.0.0` та `1.1.0` без `latest`, завантажені в Minikube + підтримка GHCR | ✅ Виконано |
| **2. Локальний кластер** | `minikube` + `kubectl`, перевірені вузол і `kube-system`, увімкнено `metrics-server` та `ingress` | ✅ Виконано |
| **3. Namespace** | Окремий `Namespace` `devops-techstore` для всіх ресурсів застосунку | ✅ Виконано |
| **4. Маніфести `k8s/`** | Один файл = один об'єкт; Deployment (≥2 репліки), Service (`ClusterIP` + `NodePort`), `ConfigMap` (env + файл), `Secret`, `Ingress`, `PVC`; `requests/limits`, `readinessProbe/livenessProbe` | ✅ Виконано |
| **5. Розгортання та перевірка** | Розгортання однією командою `kubectl apply -f k8s/`, перевірка API/UI, метрик, `ConfigMap` і `Secret` | ✅ Виконано |
| **6. Життєвий цикл** | Самовідновлення, масштабування (2→4) з балансуванням, Rolling Update (`1.0.0`→`1.1.0`) без простою, `rollout history` і Rollback (`1.1.0`→`1.0.0`) | ✅ Виконано |
| **7. Діагностика** | Симуляція `ImagePullBackOff`, інспекція `Events` через `describe`, `logs` і `exec`, виправлення та відновлення | ✅ Виконано |
| **8. Документація і Git** | Чистий `k8s/`, оновлений [`README.md`](README.md), звіт [`LAB3_REPORT.md`](LAB3_REPORT.md) | ✅ Виконано |

---

## 1. Підготовка версіонованих контейнерних образів (`1.0.0` і `1.1.0`)

Для уникнення використання нестабільного тегу `latest` зібрано та завантажено у Minikube версіоновані образи обох версій (`1.0.0` та `1.1.0`) для мікросервісів проєкту та базової БД `postgres:16-alpine`. Також у [`.github/workflows/ci.yml`](.github/workflows/ci.yml) додано підтримку Git-тегів `v*` для публікації версіонованих multi-arch образів у GHCR.

```text
$ minikube image ls | grep -E 'devops-lab1|postgres'
ghcr.io/vlkorolenko/devops-lab1/product-service:1.1.0
ghcr.io/vlkorolenko/devops-lab1/product-service:1.0.0
ghcr.io/vlkorolenko/devops-lab1/order-service:1.1.0
ghcr.io/vlkorolenko/devops-lab1/order-service:1.0.0
ghcr.io/vlkorolenko/devops-lab1/frontend:1.1.0
ghcr.io/vlkorolenko/devops-lab1/frontend:1.0.0
docker.io/library/postgres:16-alpine
```

---

## 2. Локальний кластер Minikube, вузол та аддони

```text
$ minikube status
minikube
type: Control Plane
host: Running
kubelet: Running
apiserver: Running
kubeconfig: Configured

$ kubectl get nodes -o wide
NAME       STATUS   ROLES           AGE   VERSION   INTERNAL-IP    EXTERNAL-IP   OS-IMAGE                         KERNEL-VERSION            CONTAINER-RUNTIME
minikube   Ready    control-plane   14m   v1.37.0   192.168.49.2   <none>        Debian GNU/Linux 12 (bookworm)   7.0.14-linuxkit (arm64)   containerd://2.3.4

$ kubectl get pods -n kube-system
NAME                               READY   STATUS    RESTARTS   AGE
coredns-559f6c778d-fv9zn           1/1     Running   0          14m
etcd-minikube                      1/1     Running   0          14m
kindnet-5fnx2                      1/1     Running   0          14m
kube-apiserver-minikube            1/1     Running   0          14m
kube-controller-manager-minikube   1/1     Running   0          14m
kube-proxy-wqgv4                   1/1     Running   0          14m
kube-scheduler-minikube            1/1     Running   0          14m
metrics-server-768f9f6999-t47wl    1/1     Running   0          14m
storage-provisioner                1/1     Running   0          14m

$ kubectl get pods -n ingress-nginx
NAME                                       READY   STATUS      RESTARTS      AGE
ingress-nginx-admission-create-qkk8z       0/1     Completed   0             14m
ingress-nginx-admission-patch-nwd2r        0/1     Completed   2 (10m ago)   14m
ingress-nginx-controller-d7cd8c989-bz5xt   1/1     Running     0             14m

$ minikube addons list | grep -E 'metrics-server|ingress'
│ ingress                     │ minikube │ enabled ✅ │ Kubernetes                             │
│ ingress-dns                 │ minikube │ disabled   │ minikube                               │
│ metrics-server              │ minikube │ enabled ✅ │ Kubernetes                             │

$ kubectl top nodes
NAME       CPU(cores)   CPU(%)   MEMORY(bytes)   MEMORY(%)
minikube   711m         8%       2174Mi          55%
```

---

## 3–4. Простір імен та маніфести Kubernetes (`k8s/`)

Усі об'єкти ізольовано в `Namespace` `devops-techstore` ([`k8s/00-namespace.yaml`](k8s/00-namespace.yaml)) та розбито на окремі вручну написані маніфести в каталозі [`k8s/`](k8s/) (1 файл = 1 об'єкт):

- `ConfigMap` [`techstore-config`](k8s/01-configmap.yaml): 7 параметрів оточення (`SPRING_DATASOURCE_URL`, `APP_ENVIRONMENT`, `APP_STORE_NAME`, `SPRING_JPA_SHOW_SQL`, `LOGGING_LEVEL_COM_DEVOPS_SHOP`, `PRODUCT_SERVICE_HOST`, `ORDER_SERVICE_HOST`) + файл `store-config.json`, змонтований у `frontend`.
- `Secret` [`techstore-secret`](k8s/02-secret.yaml): тестові неробочі дані `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `INTERNAL_API_KEY`.
- `PersistentVolumeClaim` [`postgres-pvc`](k8s/03-postgres-pvc.yaml) (`1Gi`, `ReadWriteOnce`) + `Deployment` [`postgres`](k8s/04-postgres-deployment.yaml) (`Recreate`, `pg_isready` проби, `subPath: pgdata`) + `ClusterIP Service` [`postgres`](k8s/05-postgres-service.yaml).
- `Deployment` [`product-service`](k8s/06-product-service-deployment.yaml) (2 репліки, `:1.0.0`, `RollingUpdate maxUnavailable=0`, проби `/actuator/health/*`, `requests/limits`) + `ClusterIP Service` [`product-service`](k8s/07-product-service-service.yaml).
- `Deployment` [`order-service`](k8s/08-order-service-deployment.yaml) (2 репліки, `:1.0.0`, `RollingUpdate maxUnavailable=0`, проби `/actuator/health/*`, `requests/limits`) + `ClusterIP Service` [`order-service`](k8s/09-order-service-service.yaml).
- `Deployment` [`frontend`](k8s/10-frontend-deployment.yaml) (2 репліки, `:1.0.0`, Nginx template з динамічним CoreDNS, монтування `store-config.json`, проби `/k8s-info`) + `NodePort Service` [`frontend`](k8s/11-frontend-service.yaml) (`80:30080`) + `Ingress` [`techstore-ingress`](k8s/12-ingress.yaml).

---

## 5. Розгортання з нуля та перевірка працездатності

### 5(a)–5(b). Розгортання однією командою та інспекція об'єктів

```text
$ kubectl apply -f k8s/
namespace/devops-techstore created
configmap/techstore-config created
secret/techstore-secret created
persistentvolumeclaim/postgres-pvc created
deployment.apps/postgres created
service/postgres created
deployment.apps/product-service created
service/product-service created
deployment.apps/order-service created
service/order-service created
deployment.apps/frontend created
service/frontend created
ingress.networking.k8s.io/techstore-ingress created

$ kubectl wait --for=condition=available deployment --all -n devops-techstore --timeout=180s
deployment.apps/frontend condition met
deployment.apps/order-service condition met
deployment.apps/postgres condition met
deployment.apps/product-service condition met

$ kubectl get pods,deployments,replicasets,services,ingress,pvc -n devops-techstore -o wide
NAME                                   READY   STATUS    RESTARTS   AGE   IP            NODE       NOMINATED NODE   READINESS GATES
pod/frontend-5d88869466-gkk4b          1/1     Running   0          73s   10.244.0.21   minikube   <none>           <none>
pod/frontend-5d88869466-hgm7f          1/1     Running   0          73s   10.244.0.22   minikube   <none>           <none>
pod/order-service-5499bc87c7-2p22q     1/1     Running   0          73s   10.244.0.20   minikube   <none>           <none>
pod/order-service-5499bc87c7-7zpfk     1/1     Running   0          73s   10.244.0.19   minikube   <none>           <none>
pod/postgres-6d6dc64d7f-5npd5          1/1     Running   0          73s   10.244.0.18   minikube   <none>           <none>
pod/product-service-545fb94449-jxpr5   1/1     Running   0          73s   10.244.0.16   minikube   <none>           <none>
pod/product-service-545fb94449-kfntg   1/1     Running   0          73s   10.244.0.17   minikube   <none>           <none>

NAME                              READY   UP-TO-DATE   AVAILABLE   AGE   CONTAINERS        IMAGES                                                  SELECTOR
deployment.apps/frontend          2/2     2            2           73s   frontend          ghcr.io/vlkorolenko/devops-lab1/frontend:1.0.0          app=frontend,tier=frontend
deployment.apps/order-service     2/2     2            2           73s   order-service     ghcr.io/vlkorolenko/devops-lab1/order-service:1.0.0     app=order-service,tier=backend
deployment.apps/postgres          1/1     1            1           73s   postgres          postgres:16-alpine                                      app=postgres,tier=database
deployment.apps/product-service   2/2     2            2           73s   product-service   ghcr.io/vlkorolenko/devops-lab1/product-service:1.0.0   app=product-service,tier=backend

NAME                                         DESIRED   CURRENT   READY   AGE   CONTAINERS        IMAGES                                                  SELECTOR
replicaset.apps/frontend-5d88869466          2         2         2       73s   frontend          ghcr.io/vlkorolenko/devops-lab1/frontend:1.0.0          app=frontend,pod-template-hash=5d88869466,tier=frontend
replicaset.apps/order-service-5499bc87c7     2         2         2       73s   order-service     ghcr.io/vlkorolenko/devops-lab1/order-service:1.0.0     app=order-service,pod-template-hash=5499bc87c7,tier=backend
replicaset.apps/postgres-6d6dc64d7f          1         1         1       73s   postgres          postgres:16-alpine                                      app=postgres,pod-template-hash=6d6dc64d7f,tier=database
replicaset.apps/product-service-545fb94449   2         2         2       73s   product-service   ghcr.io/vlkorolenko/devops-lab1/product-service:1.0.0   app=product-service,pod-template-hash=545fb94449,tier=backend

NAME                      TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)        AGE   SELECTOR
service/frontend          NodePort    10.111.202.233   <none>        80:30080/TCP   73s   app=frontend,tier=frontend
service/order-service     ClusterIP   10.104.219.111   <none>        8082/TCP       73s   app=order-service,tier=backend
service/postgres          ClusterIP   10.99.219.241    <none>        5432/TCP       73s   app=postgres,tier=database
service/product-service   ClusterIP   10.96.79.141     <none>        8081/TCP       73s   app=product-service,tier=backend

NAME                                          CLASS   HOSTS   ADDRESS        PORTS   AGE
ingress.networking.k8s.io/techstore-ingress   nginx   *       192.168.49.2   80      73s

NAME                                 STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   VOLUMEATTRIBUTESCLASS   AGE   VOLUMEMODE
persistentvolumeclaim/postgres-pvc   Bound    pvc-c399fd51-6607-49fc-8575-e66b93260f53   1Gi        RWO            standard       <unset>                 73s   Filesystem

$ kubectl get configmap,secret -n devops-techstore
NAME                         DATA   AGE
configmap/kube-root-ca.crt   1      73s
configmap/techstore-config   8      73s

NAME                      TYPE     DATA   AGE
secret/techstore-secret   Opaque   6      73s

$ kubectl top pods -n devops-techstore
NAME                               CPU(cores)   MEMORY(bytes)
frontend-5d88869466-gkk4b          10m          7Mi
frontend-5d88869466-hgm7f          8m           7Mi
order-service-5499bc87c7-2p22q     494m         136Mi
order-service-5499bc87c7-7zpfk     493m         167Mi
postgres-6d6dc64d7f-5npd5          131m         57Mi
product-service-545fb94449-jxpr5   491m         122Mi
product-service-545fb94449-kfntg   494m         127Mi
```

### 5(c). Перевірка веб-застосунку через Ingress, NodePort та PostgreSQL

```text
$ curl -i -s http://127.0.0.1:18090/k8s-info
HTTP/1.1 200 OK
Date: Wed, 11 Nov 2026 19:51:20 GMT
Content-Type: application/json
Content-Length: 89
Connection: keep-alive
X-Served-By: frontend-5d88869466-hgm7f
X-App-Version: 1.0.0
Cache-Control: no-store

{"service":"frontend","version":"1.0.0","pod":"frontend-5d88869466-hgm7f","status":"ok"}

$ curl -s http://127.0.0.1:18090/api/products
[{"id":1,"name":"Ноутбук Pro Developer 16\"","description":"Потужний ноутбук для розробки та DevOps завдань: 32GB RAM, 1TB SSD, 12-core CPU.","price":1899.99,"category":"Laptops","stock":15,...}]

$ curl -s -X POST http://127.0.0.1:18090/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Олена Коваленко","customerEmail":"olena@techstore.ua","customerAddress":"м. Київ, вул. Політехнічна, 14","items":[{"productId":1,"productName":"Ноутбук Pro Developer 16\"","price":1899.99,"quantity":1}]}'
{"id":1,"customerName":"Олена Коваленко","customerEmail":"olena@techstore.ua","customerAddress":"м. Київ, вул. Політехнічна, 14","totalAmount":1899.99,"status":"CONFIRMED","createdAt":"2026-11-11T19:51:23.323317429","items":[{"productId":1,"productName":"Ноутбук Pro Developer 16\"","price":1899.99,"quantity":1}]}

$ kubectl exec -n devops-techstore deploy/postgres -- psql -U shop_user -d shop_db -c "SELECT id, name, category, price FROM products ORDER BY id;"
 id |            name            |  category   |  price
----+----------------------------+-------------+---------
  1 | Ноутбук Pro Developer 16"  | Laptops     | 1899.99
  2 | Механічна клавіатура RGB   | Accessories |  129.50
  3 | Бездротова миша Precision  | Accessories |   69.99
  4 | 4K UltraWide Монітор 34"   | Monitors    |  649.00
  5 | Бездротові навушники з ANC | Audio       |  249.99
  6 | USB-C Док-станція 10-в-1   | Accessories |   79.90
(6 rows)
```

### 5(d). Підтвердження потрапляння `ConfigMap` та `Secret` у контейнери

```text
$ kubectl exec -n devops-techstore deploy/product-service -- env | grep -E '^(SPRING_DATASOURCE_|APP_|LOGGING_LEVEL_|INTERNAL_API_KEY)' | sort
APP_ENVIRONMENT=kubernetes-minikube
APP_STORE_NAME=DevOps TechStore (Kubernetes)
APP_VERSION=1.0.0
INTERNAL_API_KEY=k8s-demo-internal-api-key
LOGGING_LEVEL_COM_DEVOPS_SHOP=INFO
SPRING_DATASOURCE_PASSWORD=k8s-demo-db-password-2026
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/shop_db
SPRING_DATASOURCE_USERNAME=shop_user

$ kubectl exec -n devops-techstore deploy/frontend -- env | grep -E '^(APP_|PRODUCT_SERVICE_HOST|ORDER_SERVICE_HOST)' | sort
APP_ENVIRONMENT=kubernetes-minikube
APP_STORE_NAME=DevOps TechStore (Kubernetes)
ORDER_SERVICE_HOST=order-service.devops-techstore.svc.cluster.local
PRODUCT_SERVICE_HOST=product-service.devops-techstore.svc.cluster.local

$ kubectl exec -n devops-techstore deploy/frontend -- cat /usr/share/nginx/html/config/store-config.json
{
  "storeName": "DevOps TechStore (Kubernetes)",
  "environment": "minikube",
  "currency": "USD",
  "banner": "Лабораторна робота №3 • Kubernetes (minikube)"
}

$ curl -s http://127.0.0.1:18090/actuator/product-service/info
{"app":{"name":"product-service","version":"1.0.0","environment":"kubernetes-minikube","storeName":"DevOps TechStore (Kubernetes)"}}
```

---

## 6. Керування життєвим циклом застосунку

### 6(a). Самовідновлення (Self-Healing)

Видалення одного з подів `frontend` автоматично ініціює створення нового пода контролером `ReplicaSet` для підтримання `replicas: 2`:

```text
$ kubectl get pods -n devops-techstore -l app=frontend
NAME                        READY   STATUS    RESTARTS   AGE
frontend-5d88869466-gkk4b   1/1     Running   0          79s
frontend-5d88869466-hgm7f   1/1     Running   0          79s

$ kubectl delete pod frontend-5d88869466-gkk4b -n devops-techstore
pod "frontend-5d88869466-gkk4b" deleted from devops-techstore namespace

$ kubectl wait --for=condition=available deployment/frontend -n devops-techstore --timeout=60s
deployment.apps/frontend condition met

$ kubectl get pods -n devops-techstore -l app=frontend
NAME                        READY   STATUS    RESTARTS   AGE
frontend-5d88869466-hgm7f   1/1     Running   0          86s
frontend-5d88869466-zt46s   1/1     Running   0          7s
```

### 6(b). Масштабування (`2 -> 4` репліки) та балансування трафіку

Після масштабування `frontend` до 4 реплік запити через `NodePort` (`kube-proxy`) та `Ingress` (`ingress-nginx`) рівномірно розподіляються між усіма чотирма подами:

```text
$ kubectl scale deployment/frontend -n devops-techstore --replicas=4
deployment.apps/frontend scaled

$ kubectl get pods -n devops-techstore -l app=frontend -o wide
NAME                        READY   STATUS    RESTARTS   AGE     IP            NODE
frontend-5d88869466-56ndm   1/1     Running   0          13s     10.244.0.35   minikube
frontend-5d88869466-f2hn6   1/1     Running   0          2m20s   10.244.0.30   minikube
frontend-5d88869466-f2ttt   1/1     Running   0          13s     10.244.0.36   minikube
frontend-5d88869466-tsn49   1/1     Running   0          2m13s   10.244.0.31   minikube

--- NodePort 30080 (20 запитів -> розподіл між 4 подами) ---
   6 frontend-5d88869466-56ndm
   3 frontend-5d88869466-f2hn6
   8 frontend-5d88869466-f2ttt
   3 frontend-5d88869466-tsn49

--- Ingress :80 (20 запитів -> розподіл між 4 подами) ---
   5 frontend-5d88869466-56ndm
   5 frontend-5d88869466-f2hn6
   7 frontend-5d88869466-f2ttt
   3 frontend-5d88869466-tsn49

$ kubectl scale deployment/frontend -n devops-techstore --replicas=2
deployment.apps/frontend scaled
```

### 6(c) та 6(e). Rolling Update (`1.0.0 -> 1.1.0`) без переривання доступності

Під час оновлення образу `frontend` та `product-service` до версії `1.1.0` фоновий зонд безперервно опитував `/k8s-info`. Стратегія `maxUnavailable: 0, maxSurge: 1` разом із `readinessProbe` забезпечила **100% доступність (33 з 33 запитів зі статусом `HTTP 200 OK`, 0 відмов)**:

```text
$ kubectl set image deployment/frontend frontend=ghcr.io/vlkorolenko/devops-lab1/frontend:1.1.0 -n devops-techstore
deployment.apps/frontend image updated

$ kubectl rollout status deployment/frontend -n devops-techstore --timeout=90s
Waiting for deployment "frontend" rollout to finish: 1 out of 2 new replicas have been updated...
Waiting for deployment "frontend" rollout to finish: 1 old replicas are pending termination...
deployment "frontend" successfully rolled out

Rolling update probe summary:
Total requests=33, HTTP 200=33, Failures=0

First 3 requests:
19:51:39 HTTP=200 {"service":"frontend","version":"1.0.0","pod":"frontend-5d88869466-hgm7f","status":"ok"}
19:51:39 HTTP=200 {"service":"frontend","version":"1.0.0","pod":"frontend-5d88869466-hgm7f","status":"ok"}
19:51:39 HTTP=200 {"service":"frontend","version":"1.0.0","pod":"frontend-5d88869466-zt46s","status":"ok"}

Version transition sample:
30:19:51:52 HTTP=200 {"service":"frontend","version":"1.1.0","pod":"frontend-596cf9756c-tcnlw","status":"ok"}
31:19:51:52 HTTP=200 {"service":"frontend","version":"1.1.0","pod":"frontend-596cf9756c-tcnlw","status":"ok"}
32:19:51:52 HTTP=200 {"service":"frontend","version":"1.1.0","pod":"frontend-596cf9756c-76shj","status":"ok"}

$ kubectl set image deployment/product-service product-service=ghcr.io/vlkorolenko/devops-lab1/product-service:1.1.0 -n devops-techstore
$ kubectl rollout status deployment/product-service -n devops-techstore --timeout=120s
deployment "product-service" successfully rolled out

$ curl -s http://127.0.0.1:18090/actuator/product-service/info
{"app":{"name":"product-service","version":"1.1.0","environment":"kubernetes-minikube","storeName":"DevOps TechStore (Kubernetes)"}}
```

### 6(d). Історія оновлень та відкат (`1.1.0 -> 1.0.0`)

```text
$ kubectl rollout history deployment/frontend -n devops-techstore
deployment.apps/frontend
REVISION  CHANGE-CAUSE
1         <none>
2         <none>

$ kubectl rollout undo deployment/frontend -n devops-techstore
deployment.apps/frontend rolled back
deployment "frontend" successfully rolled out

$ curl -s http://127.0.0.1:18090/k8s-info
{"service":"frontend","version":"1.0.0","pod":"frontend-5d88869466-f2hn6","status":"ok"}

$ kubectl rollout undo deployment/product-service -n devops-techstore
deployment.apps/product-service rolled back
deployment "product-service" successfully rolled out

$ curl -s http://127.0.0.1:18090/actuator/product-service/info
{"app":{"name":"product-service","version":"1.0.0","environment":"kubernetes-minikube","storeName":"DevOps TechStore (Kubernetes)"}}
```

---

## 7. Діагностика проблем у кластері та відновлення

### 7(a)–7(b). Симуляція неіснуючого тегу (`ErrImagePull` / `ImagePullBackOff`) та перегляд Events

```text
$ kubectl set image deployment/frontend frontend=ghcr.io/vlkorolenko/devops-lab1/frontend:9.9.9-nonexistent -n devops-techstore
deployment.apps/frontend image updated

$ kubectl get pods -n devops-techstore -l app=frontend
NAME                        READY   STATUS         RESTARTS   AGE
frontend-5d88869466-f2hn6   1/1     Running        0          105s
frontend-5d88869466-tsn49   1/1     Running        0          98s
frontend-747f5bfb6d-n6wvr   0/1     ErrImagePull   0          5s

$ kubectl describe pod frontend-747f5bfb6d-n6wvr -n devops-techstore
Events:
  Type     Reason     Age   From               Message
  ----     ------     ----  ----               -------
  Normal   Scheduled  5s    default-scheduler  Successfully assigned devops-techstore/frontend-747f5bfb6d-n6wvr to minikube
  Normal   Pulling    4s    kubelet            spec.containers{frontend}: Pulling image "ghcr.io/vlkorolenko/devops-lab1/frontend:9.9.9-nonexistent"
  Warning  Failed     3s    kubelet            spec.containers{frontend}: Failed to pull image "ghcr.io/vlkorolenko/devops-lab1/frontend:9.9.9-nonexistent": rpc error: code = NotFound desc = failed to pull and unpack image "ghcr.io/vlkorolenko/devops-lab1/frontend:9.9.9-nonexistent": failed to resolve reference "ghcr.io/vlkorolenko/devops-lab1/frontend:9.9.9-nonexistent": ghcr.io/vlkorolenko/devops-lab1/frontend:9.9.9-nonexistent: not found
  Warning  Failed     3s    kubelet            spec.containers{frontend}: Error: ErrImagePull
  Normal   BackOff    3s    kubelet            spec.containers{frontend}: Back-off pulling image "ghcr.io/vlkorolenko/devops-lab1/frontend:9.9.9-nonexistent"
  Warning  Failed     3s    kubelet            spec.containers{frontend}: Error: ImagePullBackOff
```

### 7(c). Перегляд логів та перевірка всередині контейнера (`kubectl logs`, `kubectl exec`)

```text
$ kubectl logs -n devops-techstore deploy/product-service --tail=10
2026-11-11T19:53:48.244Z  INFO 1 --- [product-service] [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-11-11T19:53:49.851Z  INFO 1 --- [product-service] [           main] o.s.d.j.r.query.QueryEnhancerFactory     : Hibernate is in classpath; If applicable, HQL parser will be used.
2026-11-11T19:53:55.867Z  INFO 1 --- [product-service] [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 3 endpoints beneath base path '/actuator'
2026-11-11T19:53:56.185Z  INFO 1 --- [product-service] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8081 (http) with context path '/'
2026-11-11T19:53:56.290Z  INFO 1 --- [product-service] [           main] c.d.shop.product.ProductApplication      : Started ProductApplication in 32.214 seconds (process running for 35.658)

# Інтерактивний вхід у контейнер: kubectl exec -it -n devops-techstore deploy/frontend -- sh
$ kubectl exec -n devops-techstore frontend-5d88869466-f2hn6 -- sh -c 'hostname && cat /usr/share/nginx/html/config/store-config.json'
frontend-5d88869466-f2hn6
{
  "storeName": "DevOps TechStore (Kubernetes)",
  "environment": "minikube",
  "currency": "USD",
  "banner": "Лабораторна робота №3 • Kubernetes (minikube)"
}
```

### 7(d). Виправлення тегу та повернення до працездатного стану

```text
$ kubectl set image deployment/frontend frontend=ghcr.io/vlkorolenko/devops-lab1/frontend:1.0.0 -n devops-techstore
deployment.apps/frontend image updated
deployment "frontend" successfully rolled out

$ kubectl get pods,deployments -n devops-techstore
NAME                                   READY   STATUS    RESTARTS   AGE
pod/frontend-5d88869466-f2hn6          1/1     Running   0          2m23s
pod/frontend-5d88869466-tsn49          1/1     Running   0          2m16s
pod/order-service-5499bc87c7-2p22q     1/1     Running   0          5m22s
pod/order-service-5499bc87c7-7zpfk     1/1     Running   0          5m22s
pod/postgres-6d6dc64d7f-5npd5          1/1     Running   0          5m22s
pod/product-service-545fb94449-sljc8   1/1     Running   0          86s
pod/product-service-545fb94449-wqx2z   1/1     Running   0          2m9s

NAME                              READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/frontend          2/2     2            2           5m22s
deployment.apps/order-service     2/2     2            2           5m22s
deployment.apps/postgres          1/1     1            1           5m22s
deployment.apps/product-service   2/2     2            2           5m22s
```
