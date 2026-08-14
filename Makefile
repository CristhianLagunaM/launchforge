.PHONY: up down reset logs logs-backend logs-db test test-back test-front build build-back build-front

up:
	docker compose up --build

down:
	docker compose down

reset:
	docker compose down -v
	docker compose up --build

logs:
	docker compose logs -f

logs-backend:
	docker compose logs -f backend

logs-db:
	docker compose logs -f db

test: test-back test-front

test-back:
	mvn -f backend/pom.xml test

test-front:
	cd frontend && npm test -- --watch=false

build: build-back build-front

build-back:
	mvn -f backend/pom.xml clean verify

build-front:
	cd frontend && npm run build

