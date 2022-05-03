.ONESHELL:# single shell invocation for all lines in the recipe
SHELL = bash# we depend on bash expansion for e.g. queue patterns

.DEFAULT_GOAL = help


### TARGETS ###

help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'


build-server-image: ## Build server image
	@docker build . -t ui:latest

run-server: stop-server ## Run server
	@docker run --name ui  -p 15672:8888 -v /Users/mrosales/gm-work/rabbitmq-oauth2-tutorial/oauth-resource-app/public:/home/app/public  ui:latest

stop-server: ## Stop server
	@(docker stop ui || true && docker rm ui || true)

build-and-run: build-server-image run-server ## Build and run
