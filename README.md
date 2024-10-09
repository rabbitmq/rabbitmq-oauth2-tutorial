# RabbitMQ OAuth2 Tutorial

The instructions on how to configure and test OAuth 2.0 in RabbitMQ have been moved to [RabbitMQ documentation](https://www.rabbitmq.com/docs/next/oauth2-examples). This repository only maintains the configuration files and scripts referenced from the RabbitMQ documentation.

**IMPORTANT**
This branch, `next`, of this repository is meant for the RabbitMQ docs with the version `Next`. For `4.0` or earlier, check the [main](https://github.com/rabbitmq/rabbitmq-oauth2-tutorial) branch.


**Table of Contents**

* [Layout](#layout)
* [Examples](#examples)

## Layout

For each OAuth provider, there is a subfolder under `conf` folder, such as `keycloak`,
or `entra`. If the OAuth provider can be deployed locally via docker, there is also a subfolder under `bin` folder, such as `bin/keycloak`, with a `deploy` script.

### RabbitMQ Configuration file

Under each OAuth provider folder, you find either a `rabbitmq.conf` file for those OAuth providers that can be deployed locally, such as `keycloak` and `uaa`. For SaaS OAuth providers like `entra`, you find instead a `rabbitmq.conf.tmpl` file that you need to clone as `rabbitmq.conf` and replace template variables such as `{Application(client) ID}` with a real value.

### RabbitMQ TLS enabled

When the example requires RabbitMQ with TLS enabled, the corresponding `conf` folder must have a file called `requires-tls`. When you run `make start-rabbitmq`, if the key and cert have not been generated yet, the command generates one. For instance, under `conf/entra` there is a `requires-tls` file. When you deploy RabbitMQ with `MODE=entra`, a key-pair is generated under `conf/entra/certs`. In `conf/entra/rabbitmq.conf.tmpl` configuration file you can see where the certificates and key are mounted.

## Examples

### Management UI Access

* [Access management UI using OAuth 2.0 tokens](https://www.rabbitmq.com/docs/next/oauth2-examples#access-management-ui)
* [Service-Provider initiated logon](https://www.rabbitmq.com/docs/next/oauth2-examples#service-provider-initiated-logon)
* [Identity-Provider initiated logon](https://www.rabbitmq.com/docs/next/oauth2-examples#identity-provider-initiated-logon)

### Using [JWT tokens in several protocols](#access-other-protocols) to access RabbitMQ

* [Management HTTP API](https://www.rabbitmq.com/docs/next/oauth2-examples#management-http-api)
* [AMQP 0-9-1](https://www.rabbitmq.com/docs/next/oauth2-examples#amqp-protocol) (and [scopes for topic exchanges](https://www.rabbitmq.com/docs/next/oauth2-examples#using-topic-exchanges) in a separate section)
* [AMQP 1.0](https://www.rabbitmq.com/docs/next/oauth2-examples#amqp10-protocol)
* [JMS](https://www.rabbitmq.com/docs/next/oauth2-examples#jms-clients)
* [MQTT](https://www.rabbitmq.com/docs/next/oauth2-examples#mqtt-protocol)

### Signing Keys, Scope Aliases, Rich Authorization Requests

* [How to Use Advanced OAuth 2.0 Configuration](https://www.rabbitmq.com/docs/next/oauth2-examples#advanced-configuration)
* [Using a custom scope field](https://www.rabbitmq.com/docs/next/oauth2-examples#using-custom-scope-field)
* [Using multiple asymmetrical signing keys](https://www.rabbitmq.com/docs/next/oauth2-examples#using-multiple-asymmetrical-signing-keys)
* [Using scope aliases](https://www.rabbitmq.com/docs/next/oauth2-examples#using-scope-aliases)
* [Preferred username claims](https://www.rabbitmq.com/docs/next/oauth2-examples#preferred-username-claims)
* [Using Rich Authorization Requests tokens](https://www.rabbitmq.com/docs/next/oauth2-examples#use-rar-tokens)

### Examples for Specific OAuth 2.0 Identity Providers

 * [Keycloak](https://www.rabbitmq.com/docs/next/oauth2-examples-keycloak)
 * [Auth0](https://www.rabbitmq.com/oauth2-examples-auth0)
 * [Microsoft Entra ID](https://www.rabbitmq.com/docs/next/oauth2-examples-entra-id) (formerly known as Azure Active Directory)
 * [OAuth2 Proxy](https://www.rabbitmq.com/docs/next/oauth2-examples-proxy)
 * [Okta](https://www.rabbitmq.com/docs/next/oauth2-examples-okta)
 * [Google](https://www.rabbitmq.com/docs/next/oauth2-examples-google)  **NOT SUPPORTED**
 * [Multiple OAuth 2.0 servers and/or audiences](https://www.rabbitmq.com/docs/next/oauth2-examples-multiresource)
