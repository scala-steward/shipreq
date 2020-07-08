.PHONY: default build run help exec up down

INPUTS  = Dockerfile

IMAGE = shipreq/ops/cadvisor

CONTAINER = cadvisor

DOCKER_ARGS  = --name $(CONTAINER)
DOCKER_ARGS += --rm
DOCKER_ARGS += --privileged
DOCKER_ARGS += --net=host
DOCKER_ARGS += --device=/dev/kmsg
DOCKER_ARGS += -v "/:/rootfs:ro"
DOCKER_ARGS += -v "/dev/disk/:/dev/disk:ro"
DOCKER_ARGS += -v "/sys:/sys:ro"
DOCKER_ARGS += -v "/var/lib/docker/:/var/lib/docker:ro"
DOCKER_ARGS += -v "/var/run:/var/run:ro"

default: run

build: $(INPUTS)
	docker build -t $(IMAGE) .

run: build
	docker run $(DOCKER_ARGS) $(IMAGE)

help: build
	docker run $(DOCKER_ARGS) $(IMAGE) --help

exec: build
	docker run $(DOCKER_ARGS) --user root -it --entrypoint /bin/sh $(IMAGE)

up: build
	-@docker rm -f $(CONTAINER) &>/dev/null
	docker run $(DOCKER_ARGS) -d $(IMAGE)

down:
	docker rm -f $(CONTAINER)
