.PHONY: help up down build logs ps test clean

help:
	@echo "DevOps TechStore — Команди управління:"
	@echo "  make up      - Запустити всі сервіси у фоновому режимі (Docker Compose)"
	@echo "  make down    - Зупинити всі контейнери"
	@echo "  make build   - Зібрати всі Docker образи"
	@echo "  make test    - Запустити всі unit та integration тести через Maven у Docker"
	@echo "  make logs    - Переглянути логи сервісів"
	@echo "  make ps      - Переглянути статус контейнерів"
	@echo "  make clean   - Очистити контейнери, томи та кеш"

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
