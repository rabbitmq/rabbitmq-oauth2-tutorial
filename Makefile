.ONESHELL:# single shell invocation for all lines in the recipe
SHELL = bash# we depend on bash expansion for e.g. queue patterns

.DEFAULT_GOAL = help


### TARGETS ###

help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

4.24.0.tar.gz:
	@wget https://github.com/cloudfoundry/uaa/archive/4.24.0.tar.gz


install-uaac: ## Install UAA Client
	@echo "Installing uaac client on your local machine "
	@(gem list --local | grep cf-uaac || sudo gem install cf-uaac && echo "Already installed")

setup-uaa-admin-client:
	@uaac target  http://localhost:8080/uaa
	@uaac token client get admin -s adminsecret
	@uaac client update admin --authorities "clients.read clients.secret clients.write uaa.admin clients.admin scim.write scim.read uaa.resource"

setup-users-and-clients: install-uaac setup-uaa-admin-client ## create users and clients
	@./bin/setup-uaa

download-uaa: 4.24.0.tar.gz extract-uaa

start-uaa: ## Start uaa (remember to run make build-uaa if you have not done )
	@./bin/deploy-uaa

extract-uaa: 4.24.0.tar.gz
	@tar xvfz 4.24.0.tar.gz
	@mv uaa-4.24.0 uaa

stop-uaa:
	@docker kill uaa

start-rabbitmq:  ## Run RabbitMQ Server
	@./bin/deploy-rabbit

stop-rabbitmq:
	@docker stop rabbitmq

start-perftest-producer: ## Start PerfTest producer application
	@uaac token client get producer -s producer_secret
	@./bin/run-perftest producer \
		--queue "q-perf-test" \
		--producers 1 \
		--consumers 0 \
		--rate 1 \
		--flag persistent \
		--exchange "x-incoming-transaction" \
		--auto-delete "false"

start-perftest-consumer: ## Start Perftest consumer application
	@uaac token client get consumer -s consumer_secret
	@./bin/run-perftest consumer \
		--queue "q-perf-test" \
		--producers 0 \
		--consumers 1 \
		--rate 1 \
		--flag persistent \
		--exchange "x-incoming-transaction" \
		--auto-delete "false"

demo-oauth-rabbitmq/target/demo-oauth-rabbitmq-*.jar:
	@cd demo-oauth-rabbitmq; mvn clean package

start-spring-demo-oauth-cf: demo-oauth-rabbitmq/target/demo-oauth-rabbitmq-*.jar ## Start the spring-demo-auth-rabbitmq application simulating CloudFoundry env
	@./bin/run-demo-oauth-cf consumer consumer_secret

stop-all-apps: ## Stop all appications we can start with this Makefile
	@docker kill consumer producer spring-demo-oauth 2>/dev/null

pivotalrabbitmq/perf-test:latest

curl: ## Run curl with a JWT token. Syntax: make curl url=http://localhost:15672/api/overview client_id=rabbit_monitor secret=rabbit_monitor
	@uaac token client get $(client_id) -s $(secret)
	@./bin/curl_url $(client_id) $(url)

open: ## Open the browser and login the user with the JWT Token. e.g: make open username=rabbit_admin password=rabbit_admin
	@./bin/open_url $(username) $(password)

build-jms-client: ## build jms client docker image
	@(docker build jms-client/. -t jms-client)

build-uaa: download-uaa ## build uaa docker image
	@(docker build -f Dockerfile-for-uaa . -t uaa:4.24.0)

start-jms-publisher: ## start jms publisher that sends 1 message
	@uaac token client get producer -s producer_secret
	@./bin/run-jms-client producer pub

start-jms-subscriber: ## start jms subscriber
	@uaac token client get consumer -s consumer_secret
	@./bin/run-jms-client consumer sub

curl-with-token: ## Run curl with a JWT token. Syntax: make curl-with-extra-scopes url=http://localhost:15672/api/overview token=....
	@curl -u :$(token) $(url)

get-jwt-token: ## Get a JWT token from an authorzation server
	@curl curl --request POST \
  --url 'https://<YOUR TOKEN ENDPOINT>' \
  --header 'content-type: application/x-www-form-urlencoded' \
  --data grant_type=client_credentials \
  --data client_id=<your-client-id> \
  --data client_secret=<your-client-secret> \
  --data audience="rabbitmq:15672"

start-mqtt-publish: ## publish mqtt message . e.g. make start-mqtt-publish token=$(bin/jwt_token legacy-token-key private.pem public.pem)
		@(docker run --rm -it --network rabbitmq_net ruimarinho/mosquitto mosquitto_pub \
		  -h rabbitmq -u "" -P $(token) -t test -m hello-world)
