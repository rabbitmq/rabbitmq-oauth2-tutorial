.ONESHELL:# single shell invocation for all lines in the recipe
SHELL = bash# we depend on bash expansion for e.g. queue patterns

.DEFAULT_GOAL = help


### TARGETS ###

help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'


4.24.0.tar.gz:
	@wget https://github.com/cloudfoundry/uaa/archive/4.24.0.tar.gz
	@tar xvfz 4.24.0.tar.gz
	@mv uaa-4.24.0 uaa

install-uaac: ## Install UAA Client
	@sudo gem install cf-uaac

setup-uaa-admin-client:
	@uaac target  http://localhost:8080/uaa
	@uaac token client get admin -s adminsecret
	@uaac client update admin --authorities "clients.read clients.secret clients.write uaa.admin clients.admin scim.write scim.read uaa.resource"

setup-users-and-tokens: install-uaac setup-uaa-admin-client ## create users and obtain tokens for them
	@./setup-uaa

uaa: 4.24.0.tar.gz

start-uaa: uaa ## Install and run uaa
	@./init-docker-network
	@docker run  --rm \
		--name uaa \
		--network oauth2 \
		-v $(CURDIR)/uaa:/uaa \
		-p 8080:8080 \
		-w /uaa \
		openjdk:8-jdk \
		./gradlew run
	@echo "Monitor the logs (docker logs uaa -f). UAA will be ready when you see 'Task :cargoRunLocal' "
	@echo "Once UAA is ready; run 'make setup-users-and-tokens' before starting any application and/or rabbitmq"

stop-uaa:
	@docker kill uaa

get-plugin:
	@wget https://github.com/rabbitmq/rabbitmq-auth-backend-oauth2/archive/master.zip
	@rm -rf rabbitmq-auth-backend-oauth2-master
	@unzip master.zip
	@rm master.zip

build-plugin: get-plugin
	@cd rabbitmq-auth-backend-oauth2-master; \
		make; \
		make dist

rabbitmq-auth-backend-oauth2-master/plugins/rabbitmq_auth*.ez: build-plugin

.built-rabbitmq-docker: rabbitmq-auth-backend-oauth2-master/plugins/rabbitmq_auth*.ez ## Build RabbitMQ docker image
	@rm -rf plugin
	@mkdir plugin
	@cp rabbitmq-auth-backend-oauth2-master/plugins/rabbitmq_auth*.ez plugin
	@cp rabbitmq-auth-backend-oauth2-master/plugins/base64url-*.ez plugin
	@cp rabbitmq-auth-backend-oauth2-master/plugins/jose-*.ez plugin
	@docker build -t rabbitmq-oauth2 -f rabbitmq-Dockerfile .
	@touch .built-rabbitmq-docker

start-rabbitmq: .built-rabbitmq-docker ## Run RabbitMQ Server
	@cp rabbitmq.config plugin
	@cp enabled_plugins plugin
	@./init-docker-network
	@docker run -d --rm \
		--name rabbitmq \
		--network oauth2 \
		-v $(CURDIR)/plugin:/etc/rabbitmq \
		-p 15672:15672 \
		-p 5672:5672 \
		rabbitmq-oauth2

stop-rabbitmq:
	@docker stop rabbitmq

start-perftest-producer: ## Start PerfTest producer application
	@./init-docker-network
	@uaac token client get producer -s producer_secret
	@./run-perftest producer \
		--queue "q-perf-test" \
		--producers 1 \
		--consumers 0 \
		--rate 1 \
		--flag persistent \
		--exchange "x-incoming-transaction" \
		--auto-delete "false"

start-perftest-consumer: ## Start Perftest consumer application
	@./init-docker-network
	@uaac token client get consumer -s consumer_secret
	@./run-perftest consumer \
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
	@./init-docker-network
	@./run-demo-oauth-cf consumer consumer_secret

stop-all-apps: ## Stop all appications we can start with this Makefile
	@docker kill consumer producer spring-demo-oauth 2>/dev/null

pivotalrabbitmq/perf-test:latest

curl: ## Run curl with a JWT token. Syntax: make curl url=http://localhost:15672/api/overview as=rabbit_admin
	@./curl_url $(as) $(url)

open: ## Open the browser and login the user with the JWT Token. e.g: make open username=rabbit_admin password=rabbit_admin
	@./open_url $(username) $(password)
