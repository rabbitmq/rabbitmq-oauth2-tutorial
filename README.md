# RabbitMQ OAuth2 Tutorial

The instructions on how to configure and test RabbitMQ with various OAuth 2.0 providers have been moved to [RabbitMQ documentation](https://www.rabbitmq.com/docs/oauth2-examples). This repository only maintains the configuration files referenced from the RabbitMQ documentation and the
scripts used in some of the examples.

## Layout

For each OAuth provider, there is a subfolder under `conf` folder, such as `keycloak`,
or `entra` for Microsoft Entry ID. If the OAuth provider can be deployed locally via docker,
such as `keycloak` or `uaa`, there is a subfolder under `bin` folder with a `deploy` script.

When the example requires RabbitMQ listens on HTTPS port, the corresponding `conf` folder must have a file called `requires-tls`. When you run `make start-rabbitmq`, if the key and cert have not been generated yet, the command generates one. For instance, under `conf/entra` there is a `requires-tls` file. When you deploy RabbitMQ with `MODE=entra`, a key-pair is generated under `conf/entra/certs`. In `conf/entra/rabbitmq.conf`
configuration file you can see where the certificates and key are mounted.
