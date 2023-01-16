.ONESHELL:# single shell invocation for all lines in the recipe
SHELL = bash# we depend on bash expansion for e.g. queue patterns

.DEFAULT_GOAL = help
PRODUCER := producer
CONSUMER := consumer


### TARGETS ###

help:
	@grep -E '^[0-9a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

install-uaac: ## Install UAA Client
	@echo "Installing uaac client on your local machine "
	@(gem list --local | grep cf-uaac || sudo gem install cf-uaac && echo "Already installed")

start-uaa: ## Start uaa (remember to run make build-uaa if you have not done )
	@./bin/uaa/deploy

start-keycloak: ## Start keycloak
	@./bin/keycloak/deploy

build-azure: ##  Generate SSL files for Azure AD
	@./bin/azure/deploy

stop-uaa: ## Stop uaa
	@docker kill uaa

stop-keycloak: ## Stop keycloak
	@docker kill keycloak

start-rabbitmq:  ## Run RabbitMQ Server
	@./bin/deploy-rabbit

stop-rabbitmq:
	@docker stop rabbitmq

start-perftest-producer: ## Start PerfTest producer application
	@uaac token client get $(PRODUCER) -s $(PRODUCER)_secret
	@./bin/run-perftest $(PRODUCER) \
		--queue "q-perf-test" \
		--producers 1 \
		--consumers 0 \
		--rate 1 \
		--flag persistent \
		--exchange "x-incoming-transaction" \
		--auto-delete "false"

start-perftest-producer-with-token: ## Start PerfTest producer application with a token
	@TOKEN=$(TOKEN) ./bin/run-perftest $(PRODUCER)\
		--queue "q-perf-test" \
		--producers 1 \
		--consumers 0 \
		--rate 1 \
		--flag persistent \
		--exchange "x-incoming-transaction" \
		--auto-delete "false"

stop-perftest-producer: ## Stop perfTest producer
	@docker stop $(PRODUCER)

start-perftest-consumer: ## Start Perftest consumer application
	@uaac token client get $(CONSUMER) -s $(CONSUMER)_secret
	@./bin/run-perftest $(CONSUMER) \
		--queue "q-perf-test" \
		--producers 0 \
		--consumers 1 \
		--rate 1 \
		--flag persistent \
		--exchange "x-incoming-transaction" \
		--auto-delete "false"

start-perftest-consumer-with-token: ## Start Perftest consumer application with a token
	@TOKEN=$(TOKEN) ./bin/run-perftest $(CONSUMER) \
		--queue "q-perf-test" \
		--producers 0 \
		--consumers 1 \
		--rate 1 \
		--flag persistent \
		--exchange "x-incoming-transaction" \
		--auto-delete "false"

stop-perftest-consumer: ## Stop perfTest consumer
	@docker stop $(CONSUMER)


stop-all-apps: ## Stop all appications we can start with this Makefile
	@docker kill consumer producer 2>/dev/null

pivotalrabbitmq/perf-test:latest

curl-uaa: ## Run curl with a JWT token. Syntax: make curl-uaa url=http://localhost:15672/api/overview client_id=rabbit_monitor secret=rabbit_monitor
	@uaac token client get $(client_id) -s $(secret)
	@./bin/uaa/curl_url $(client_id) $(url)

curl-keycloak: ## Run curl with a JWT token. Syntax: make curl-keycloak url=http://localhost:15672/api/overview client_id=rabbit_monitor secret=rabbit_monitor
	@./bin/keycloak/curl $(url) $(client_id) $(secret)

open: ## Open the browser and login the user with the JWT Token. e.g: make open username=rabbit_admin password=rabbit_admin
	@./bin/open_url $(username) $(password)

build-jms-client: ## build jms client docker image
	@(docker build jms-client/. -t jms-client)

start-jms-publisher: ## start jms publisher that sends 1 message
	@uaac token client get jms_producer -s jms_producer_secret
	@./bin/run-jms-client jms_producer pub

start-jms-subscriber: ## start jms subscriber
	@uaac token client get jms_consumer -s jms_consumer_secret
	@./bin/run-jms-client jms_consumer sub

build-amqp1_0-client: ## build amqp1_0 client docker image
	@(docker build amqp1_0-client/. -t amqp1_0-client)

start-amqp1_0-publisher: ## start amqp publisher that sends 1 message
	@uaac token client get producer -s producer_secret
	@./bin/run-amqp1_0-client producer pub

start-amqp1_0-subscriber: ## start amqp subscriber
	@uaac token client get consumer -s consumer_secret
	@./bin/run-amqp1_0-client consumer sub

curl-with-token: ## Run curl with a JWT token. Syntax: make curl-with-extra-scopes URL=http://localhost:15672/api/overview TOKEN=....
	@curl -u :$(TOKEN) $(URL)

get-jwt-token: ## Get a JWT token from an authorzation server
	@curl curl --request POST \
  --url 'https://<YOUR TOKEN ENDPOINT>' \
  --header 'content-type: application/x-www-form-urlencoded' \
  --data grant_type=client_credentials \
  --data client_id=<your-client-id> \
  --data client_secret=<your-client-secret> \
  --data audience="rabbitmq:15672"

start-mqtt-publish: ## publish mqtt message . e.g. make start-mqtt-publish TOKEN=$(bin/jwt_token legacy-token-key private.pem public.pem)
		@(docker run --rm -it --network rabbitmq_net ruimarinho/mosquitto mosquitto_pub \
		  -h rabbitmq -u "" -P $(TOKEN) -t test -m hello-world)
