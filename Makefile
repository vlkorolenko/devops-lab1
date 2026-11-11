.PHONY: help up down build logs ps test clean k8s-up k8s-down k8s-status

help:
	@echo "DevOps TechStore — Команди управління:"
	@echo "  make up         - Запустити всі сервіси у фоновому режимі (Docker Compose)"
	@echo "  make down       - Зупинити всі контейнери"
	@echo "  make build      - Зібрати всі Docker образи"
	@echo "  make test       - Запустити всі unit та integration тести через Maven у Docker"
	@echo "  make logs       - Переглянути логи сервісів"
	@echo "  make ps         - Переглянути статус контейнерів"
	@echo "  make clean      - Очистити контейнери, томи та кеш"
	@echo "  make k8s-up     - Розгорнути всі маніфести Kubernetes з k8s/"
	@echo "  make k8s-status - Показати об'єкти у namespace devops-techstore"
	@echo "  make k8s-down   - Видалити всі об'єкти Kubernetes з k8s/"

up:
	docker compose up -d --build

down:
	docker compose down

build:
	docker compose build

test:
	./run_tests.sh

logs:
	docker compose logs -f

ps:
	docker compose ps

clean:
	docker compose down -v --remove-orphans

k8s-up:
	kubectl apply -f k8s/

k8s-status:
	kubectl get all,ingress,pvc,cm,secret -n devops-techstore

k8s-down:
	kubectl delete -f k8s/ --ignore-not-found
