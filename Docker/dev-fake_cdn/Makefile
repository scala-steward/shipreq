IMAGE      := shipreq/dev/fake_cdn
CONTAINER  := shipreq_fake_cdn
DEV_DOMAIN := http://localhost:14080
TEST_RES   := ef085477e7327cc3191ec97fda7e72f6.js
TEST_POST  := | grep '^<' | sort

DOCKER_ARGS  = --name $(CONTAINER)
DOCKER_ARGS += --network host # so that we can access :14080
DOCKER_ARGS += -it # so that ctrl-c works
DOCKER_ARGS += --rm
DOCKER_ARGS += -e TARGET=$(DEV_DOMAIN)

CURL  = curl
CURL += -v
CURL += -H 'origin: $(DEV_DOMAIN)'
CURL += -H 'referer: $(DEV_DOMAIN)/project/TeSt'
CURL += -s -o /dev/null 2>&1

default: run

build:
	docker build -t $(IMAGE) .

run: build
	docker run $(DOCKER_ARGS) $(IMAGE)

rm:
	docker rm -f $(CONTAINER)

run-native:
	TARGET=$(DEV_DOMAIN) node app.js

test:
	@$(CURL) http://localhost:6666/$(TEST_RES) $(TEST_POST)
