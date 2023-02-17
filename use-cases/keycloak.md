# Use KeyCloak as OAuth 2.0 server

You are going to test 3 OAuth flows:
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

There is a dedicated **KeyCloak realm** called `Test` configured as follows:
- You configured an [rsa](http://0.0.0.0:8080/admin/master/console/#/realms/test/keys) signing key
- And a [rsa provider](http://0.0.0.0:8080/admin/master/console/#/realms/test/keys/providers)
- And three clients: `rabbitmq-client-code` for the rabbitmq managament ui, `mgt_api_client` to access via the
management api and `producer` to access via AMQP protocol.


## Start RabbitMQ

To start RabbitMQ run the following two commands. The first one tells RabbitMQ to pick up the
rabbit.config found under [conf/keycloak/rabbitmq.config](conf/keycloak/rabbitmq.config)
```
export MODE=keycloak
make start-rabbitmq
```

## Access Management ui

Go to http://localhost:15672/#/, click on `Click here to log in` button, and enter the credentials
`rabbit_admin` as username and `rabbit_admin` as password. This is the single user created in keycloak with the
appropriate scopes to access the management ui.

## Access Management api

Access the management api using the client [mgt_api_client](http://0.0.0.0:8080/admin/master/console/#/realms/test/clients/c5be3c24-0c88-4672-a77a-79002fcc9a9d) which has the scope [rabbitmq.tag:administrator](http://0.0.0.0:8080/admin/master/console/#/realms/test/client-scopes/f6e6dd62-22bf-4421-910e-e6070908764c)

```
make curl-keycloak url=http://localhost:15672/api/overview client_id=mgt_api_client secret=LWOuYqJ8gjKg3D2U8CJZDuID3KiRZVDa
```

## Access AMQP protocol with PerfTest

To test OAuth2 authentication with AMQP protocol you are going to use RabbitMQ PerfTest tool which uses RabbitMQ Java Client.
First you obtain the token and pass it as a parameter to the make target `start-perftest-producer-with-token`.

```
make start-perftest-producer-with-token PRODUCER=producer TOKEN=$(bin/keycloak/token producer kbOFBXI9tANgKUq8vXHLhT6YhbivgXxn)
```

**NOTE**: Initializing an application with a token has one drawback: the application cannot use the connection beyond the lifespan of the token. See the next section where you demonstrate how to refresh the token.

## Access AMQP protocol with Pika

This section is about testing Oauth2 authentication with AMQP protocol and with Pika library. And more specifically, you
are demonstrating how to refresh a token on a live AMQP connections.

You can see the Python sample application [here](../pika_keycloak).

To run this sample code proceed as follows:
```
python3 --version
pip install pika
pip install requests
python3 pika-client/producer.py producer kbOFBXI9tANgKUq8vXHLhT6YhbivgXxn
```
> Ensure you install pika 1.3

## Access Management UI

Go to http://localhost:15672, click on the single button on the page which redirects to **Key Cloak** to authenticate.
Enter `rabbit_admin` and `rabbit_admin` and you should be redirected back to RabbitMQ Management fully logged in.


## Stop keycloak

`make stop-keycloak`


## Notes about setting up KeyCloak

### Configure JWT signing Keys

At the realm level, you go to `Keys > Providers` tab and create one of type `rsa` and you enter the
private key and certificate of the public key. In this repository you do not have yet the certificate
for the public key but it is easy to generate. Give it priority `101` or greater than the rest of
available keys so that it is picked up when you request a token.

IMPORTANT: You cannot hard code the **kid** hence you have to add the key to rabbitmq via the command
```
docker exec -it rabbitmq rabbitmqctl add_uaa_key Gnl2ZlbRh3rAr6Wymc988_5cY7T5GuePd5dpJlXDJUk --pem-file=conf/public.pem
```
or you have to modify the RabbitMQ configuration so that it says `Gnl2ZlbRh3rAr6Wymc988_5cY7T5GuePd5dpJlXDJUk`
rather than `legacy-token-key`.

### Configure Client

For backend applications which uses **Client Credentials flow** you create a **Client** with:
- **Access Type** : `public`
- **Authentication flow** : `Standard Flow`
- With **Service Accounts Enabled** on. If it is not enabled you do not have the tab `Credentials`
- In tab `Credentials` you have the client id secret


### Configure Client scopes

> *Default Client Scope* are scopes automatically granted to every token. Whereas *Optional Client Scope* are
scopes which are only granted if they are explicitly requested during the authorization/token request flow.


### Include appropriate aud claim

You must configure a **Token Mapper** of type **Hardcoded claim** with the value of rabbitmq's *resource_server_id**.
You can configure **Token Mapper** either to a **Client scope** or to a **Client**.

### Export Keycloak configuration
For testing purposes, once you modified keycloak configuration, you would want to export keycloak configuration.
When done, connect to the keycloak container and export your configuration before removing the container
> The following command overrides the default configuration provided with this repository
```shell
docker exec -it keycloak /opt/keycloak/bin/kc.sh export --realm test --dir /opt/keycloak/data/import/ --users realm_file
```

### Possible issue on MacOS

If you want to run this configuration on MacOS, you could have problem reaching keycloak pointing to 0.0.0.0:8080.

1. add `keycloak` entry in your `hosts` file
```shell
echo "127.0.0.1 keycloak" > /etc/hosts
```
1. modify the [rabbitmq.config](../conf/keycloak/rabbitmq.config) configuration to appropriately point to `keycloak` host
```shell
...
{oauth_provider_url, "http://keycloak:8080/realms/test"}
...
```
