.PHONY: build test run image push format local-up local-down

build:
	mvn -B clean install

test:
	mvn -B test

run:
	mvn -B spring-boot:run -Dspring-boot.run.profiles=local

image:
	mvn -B jib:dockerBuild

push:
	mvn -B jib:build

format:
	mvn -B spotless:apply

local-up:
	docker compose up -d --wait

local-down:
	docker compose down
