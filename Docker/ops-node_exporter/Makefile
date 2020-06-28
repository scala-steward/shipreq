.PHONY: default build run help up down

INPUTS  = Dockerfile

IMAGE = shipreq/ops/node_exporter

CONTAINER = node_exporter

DOCKER_ARGS  = --name $(CONTAINER)
DOCKER_ARGS += --rm
DOCKER_ARGS += --privileged
DOCKER_ARGS += --net=host
DOCKER_ARGS += --pid=host
DOCKER_ARGS += -v "/:/host:ro,rslave"

APP_ARGS  = --path.rootfs=/host
APP_ARGS += --path.procfs=/host/proc
APP_ARGS += --path.sysfs=/host/sys
APP_ARGS += '--collector.filesystem.ignored-mount-points=^/(dev|proc|sys|var/lib/docker/.+|host|etc)($$|/)'

default: run

build: $(INPUTS)
	docker build -t $(IMAGE) .

run: build
	docker run $(DOCKER_ARGS) $(IMAGE) $(APP_ARGS)

help: build
	docker run $(DOCKER_ARGS) $(IMAGE) --help

up: build
	-@docker rm -f $(CONTAINER) &>/dev/null
	docker run $(DOCKER_ARGS) -d $(IMAGE) $(APP_ARGS)

down:
	docker rm -f $(CONTAINER)
