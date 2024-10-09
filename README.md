# RabbitMQ OAuth2 Tutorial

The instructions on how to configure and test OAuth 2.0 in RabbitMQ have been moved to [RabbitMQ documentation](https://www.rabbitmq.com/docs/oauth2-examples). This repository only maintains the configuration files and scripts referenced from the RabbitMQ documentation.


## Layout

For each OAuth provider, there is a subfolder under `conf` folder, such as `keycloak`,
or `entra`. If the OAuth provider can be deployed locally via docker, there is a subfolder under `bin` folder with a `deploy` script.

### RabbitMQ Configuration file

Under each OAuth provider folder, you find either a `rabbitmq.conf` file for those OAuth providers that can be deployed locally, such as `keycloak` and `uaa`. For SaaS OAuth providers like `entra`, you find instead a `rabbitmq.conf.tmpl` file that you need to clone as `rabbitmq.conf` and replace template variables such as `{Application(client) ID}` with a real value.

### RabbitMQ TLS enabled

When the example requires RabbitMQ with TLS enabled, the corresponding `conf` folder must have a file called `requires-tls`. When you run `make start-rabbitmq`, if the key and cert have not been generated yet, the command generates one. For instance, under `conf/entra` there is a `requires-tls` file. When you deploy RabbitMQ with `MODE=entra`, a key-pair is generated under `conf/entra/certs`. In `conf/entra/rabbitmq.conf.tmpl` configuration file you can see where the certificates and key are mounted.
