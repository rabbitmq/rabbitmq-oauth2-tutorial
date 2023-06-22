# Use Google as OAuth 2.0 server

You are going to test 3 OAuth flows:
1. Access management ui via a browser :construction:
2. Access management rest api :construction:
3. Access AMQP protocol :construction:

## Prerequisites to follow this guide

- Have an account in Google.
- Docker
- Openssl

## Register your app

WIP

**NOTE**: There is a blocker issue: Google access token is not a JWT token but an opaque one.
RabbitMQ has to download the actual JWT token thru the `tokeninfo` endpoint. More info [here](https://cloud.google.com/docs/authentication/token-types#access).


## Start RabbitMQ

Run the following commands to run RabbitMQ docker image:

```
export MODE=google
make start-rabbitmq
```
