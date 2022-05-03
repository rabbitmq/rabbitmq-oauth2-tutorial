.ONESHELL:# single shell invocation for all lines in the recipe
SHELL = bash# we depend on bash expansion for e.g. queue patterns

.DEFAULT_GOAL = help

UAA_VERSION := 75.19.0

### TARGETS ###

help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

build-uaa:  ## build uaa docker image
	@(docker build -f Dockerfile . \
			--build-arg UAA_VERSION=${UAA_VERSION} \
			--tag uaa:${UAA_VERSION})
	@(docker tag uaa:${UAA_VERSION} uaa:latest)
