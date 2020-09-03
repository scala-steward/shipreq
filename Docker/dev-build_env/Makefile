.PHONY: default build exec

INPUTS  = Dockerfile strip-ansi .bashrc

IMAGE = shipreq/dev/build_env

CONTAINER = shipreq_build_env

PWD := $(shell dirname $(realpath $(firstword $(MAKEFILE_LIST))))

DOCKER_ARGS  = --name $(CONTAINER)
DOCKER_ARGS += --rm
DOCKER_ARGS += --privileged
DOCKER_ARGS += --network host
DOCKER_ARGS += -v /var/run/docker.sock:/var/run/docker.sock
DOCKER_ARGS += -v "$(PWD)/../../Code":/Code:ro
DOCKER_ARGS += -v "$(HOME)/.cache/coursier":/root/.cache/coursier:ro
DOCKER_ARGS += -v "$(HOME)/.aws":/root/.aws:ro
DOCKER_ARGS += -e SHIPREQ_TARGET=/tmp/shipreq.sbt
DOCKER_ARGS += -e SHIPREQ_RELEASE_TARGET=/tmp/shipreq-release.sbt
DOCKER_ARGS += -e DOCKER_PUSH=0
DOCKER_ARGS += --cpuset-cpus 0-7
DOCKER_ARGS += -m 15G
DOCKER_ARGS += -w /Code

default: exec

build: $(INPUTS)
	docker build \
	--cache-from $(IMAGE):latest \
	--cache-from 930801536115.dkr.ecr.ap-southeast-2.amazonaws.com/$(IMAGE):latest \
	-t $(IMAGE) .

exec:
	docker run $(DOCKER_ARGS) -it $(IMAGE) bash

exec2:
	docker run $(DOCKER_ARGS) -it 930801536115.dkr.ecr.ap-southeast-2.amazonaws.com/$(IMAGE) bash

shell:
	docker exec -it $(CONTAINER) bash
