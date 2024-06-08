# Use OAuth2 Proxy and Keycloak as OAuth 2.0 server

You are going to test the following flow: Access management ui via a browser thru OAuth2-Proxy


```
                    [ Keycloak ] 3. authenticate
                      /|\  |
                       |   | 4. token
        2.redirect     |  \|/                                        [ RabbitMQ ]
                [ Oauth2-Proxy ]       ----5. forward with token-->  [  http    ]
                      /|\
                       |
            1. rabbit_admin from a browser
```

## Prerequisites to follow this guide

- Docker
- make

## Deploy Keycloak

First, deploy **Keycloak**. It comes preconfigured with all the required scopes, users and clients.
```
make start-keycloak
```
**Keycloak** comes configured with its own signing key. And [rabbitmq.conf](../conf/oauth2-proxy/rabbitmq.conf)
is also configured with the same signing key.

To access Keycloak management interface go to http://0.0.0.0:8080/ and enter `admin` as username and password.

There is a dedicated **Keycloak realm** called `Test` configured as follows:
- [rsa](http://0.0.0.0:8080/admin/master/console/#/realms/test/keys) signing key
- [rsa provider](http://0.0.0.0:8080/admin/master/console/#/realms/test/keys/providers)
- `rabbitmq-proxy-client` client

## Start RabbitMQ

To start RabbitMQ run the following two commands. The first one tells RabbitMQ to pick up the
rabbit.conf found under [conf/oauth2-proxy/rabbitmq.conf](../conf/keycloak/rabbitmq.conf)
```
export MODE=oauth2-proxy
make start-rabbitmq
```

**NOTE**: Oauth2-proxy requires that the `aud` claim matches the client's id. However, RabbitMQ requires the
`aud` field to match `rabbitmq` which is the designated `resource_server_id`. Given that it has been
impossible to configure keycloak with both values, [rabbitmq.conf](../conf/oauth2-proxy/rabbitmq.conf) has
the setting below which disables validation of the audience claim.
```
auth_oauth2.verify_aud = false
```

**NOTE**: [rabbitmq.config](../conf/oauth2-proxy/rabbitmq.config) file is only for reference. It is not used
by `make start-rabbitmq` unless we explicitly ask for it. 

## Start OAuth2 Proxy

To start Oauth2-proxy we run the following command:
```
make start-oauth2-proxy
```
Oauth2-proxy is configured using [Alpha configuration](../conf/oauth2-proxy/alpha-config.yaml). This type of configuration permits injecting the access token into the HTTP **Authorization** header.


## Access Management ui

Go to http://0.0.0.0:4180/, click on the link **Sign in with Keycloak OIDC**, and enter the credentials
`rabbit_admin` as username and `rabbit_admin` as password. You should be redirected to RabbitMQ management ui.
