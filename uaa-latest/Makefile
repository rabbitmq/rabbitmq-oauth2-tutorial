.ONESHELL:# single shell invocation for all lines in the recipe
SHELL = bash# we depend on bash expansion for e.g. queue patterns

.DEFAULT_GOAL = help

UAA_VERSION := 75.18.0

### TARGETS ###

help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

uaa.tar.gz:
	@wget https://github.com/cloudfoundry/uaa/archive/$(UAA_VERSION).tar.gz -O uaa.tar.gz

download-uaa: uaa.tar.gz extract-uaa

extract-uaa: uaa.tar.gz
	@tar xvfz uaa.tar.gz
	@mv uaa-$(UAA_VERSION) uaa

build-uaa: download-uaa ## build uaa docker image
	@(docker build -f Dockerfile . -t uaa:${UAA_VERSION})
	@(docker tag uaa:${UAA_VERSION} uaa:latest)
	@rm -r uaa
	@rm uaa.tar.gz
