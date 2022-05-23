# Use KeyCloak as OAuth 2.0 server

We are going to test 3 OAuth flows:
1. Access management ui via a browser
2. Access management rest api
3. Access AMQP protocol

## Prerequisites to follow this guide

- Docker
- make

## Deploy Key Cloak

First, deploy **Key Cloak**. It comes preconfigured with all the required scopes, users and clients.
```
make start-keycloak
```
**Key Cloak** comes configured with its own signing key. And the [rabbitmq.config](conf/keycloak/rabbitmq.config)
used by `make start-keycloak` is also configured with the same signing key.

To access KeyCloak management interface go to http://0.0.0.0:8080/ and enter `admin` as username and password.

There is a dedicated **KeyCloak realm** called `Test` for this use case configured as follows:
- We configured an [rsa](http://0.0.0.0:8080/admin/master/console/#/realms/test/keys) signing key
- And a [rsa provider](http://0.0.0.0:8080/admin/master/console/#/realms/test/keys/providers)
- And three clients: `rabbitmq-client-code` for the rabbitmq managament ui, `mgt_api_client` to access via the
management api and `producer` to access via AMQP protocol.


## Start RabbitMQ

Next, launch RabbitMQ configured to use **Key Cloak** as its Oauth2 server:
```
CONFIG=keycloak/rabbitmq.conf make start-rabbitmq
```
> CONFIG is relative to conf folder

**NOTE**: To run RabbitMQ directly from source:
1. git clone rabbitmq/rabbitmq-server
2. git checkout oidc-integration
3. `gmake run-broker PLUGINS="rabbitmq_management rabbitmq_auth_backend_oauth2" RABBITMQ_CONFIG_FILE=<root folder of the tutorial>/conf/keycloak/rabbitmq.config`

## Access Management api

Access the management api using the client [mgt_api_client](http://0.0.0.0:8080/admin/master/console/#/realms/test/clients/c5be3c24-0c88-4672-a77a-79002fcc9a9d) which has the scope [rabbitmq.tag:administrator](http://0.0.0.0:8080/admin/master/console/#/realms/test/client-scopes/f6e6dd62-22bf-4421-910e-e6070908764c)

```
make curl-keycloak url=http://localhost:15672/api/overview client_id=mgt_api_client secret=LWOuYqJ8gjKg3D2U8CJZDuID3KiRZVDa
```

## Access AMQP protocol

```
make start-perftest-producer-with-token PRODUCER=producer TOKEN=$(bin/keycloak/token producer kbOFBXI9tANgKUq8vXHLhT6YhbivgXxn)
```

## Access Management UI

Go to http://localhost:15672, click on the single button on the page which redirects to **Key Cloak** to authenticate.
Enter `rabbit_admin` and `rabbit_admin` and you should be redirected back to RabbitMQ Management fully logged in.


## Stop keycloak

`make stop-keycloak`
