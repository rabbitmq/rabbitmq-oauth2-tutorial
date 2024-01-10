# Expose multiple OAuth 2.0 resource(s)/audience(s) with one or many oauth providers

You are going to test the following OAuth flows:
1. Access AMQP protocol
2. Access Management UI

**NOTE** : This use case deploys a RabbitMQ docker image built from a PR which is still in progress.
The docker image is **pivotalrabbitmq/rabbitmq:create-oauth2-client-multi-resource-otp-min-bazel**

## Prerequisites to follow this guide

- Docker
- `/etc/hosts` must have the following entries. This is necessary if we want to access the management ui via the browser
```
127.0.0.1 keycloak uaa keycloak1 keycloak2
::1 keycloak uaa keycloak1 keycloak2
```

## Motivation

All the examples and use-cases demonstrated by this tutorial, except for this use case, configure a single **resource_server_id** and therefore a single **OAuth 2.0 server**.

Each of the following sections below demonstrate how to configure RabbitMQ to handle more than one
oauth2 resource/audience where users and clients are declared in one or many oauth providers.

## Scenario 1 - Many OAuth 2.0 resources

In this scenarios, we have the following OAuth clients declared on a single oauth2 provider called `keycloak`:
- `prod_producer` this is an OAuth client_id used by a producer application which accesses RabbitMQ with the audience `rabbit_prod`
- `rabbit_prod_admin` this is a management user which access RabbitMQ via the resource/audience `rabbit_dev`
- `dev_producer` this is an OAuth client_id used by a producer application which accesses RabbitMQ with the audience `rabbit_dev`
- `rabbit_dev_admin` this is a management user which access RabbitMQ via the resource/audience `rabbit_dev`

Follow these steps to deploy Keycloak and RabbitMQ:
1. Launch Keycloak
```
make start-keycloak
```
2. Launch RabbitMQ
```
MODE=multi-keycloak CONF=rabbitmq.multiresource.conf make start-rabbitmq
```
3. Launch AMQP producer registered in Keycloak with the **client_id** `prod_producer` and with the permission to access `rabbit_prod` resource and with the scopes `rabbitmq.read:*/* rabbitmq.write:*/* rabbitmq.configure:*/*`:
```
make start-perftest-producer-with-token PRODUCER=prod_producer TOKEN=$(bin/keycloak/token prod_producer PdLHb1w8RH1oD5bpppgy8OF9G6QeRpL9)
```
4. Launch AMQP producer registered in Keycloak with the **client_id** `dev_producer` and with the permission to access `rabbit_dev` resource and with the scopes `rabbitmq.read:*/* rabbitmq.write:*/* rabbitmq.configure:*/*`:


### Test applications accessing AMQP protocol with their own audience

The setup: (This is the [rabbitmq.conf](../conf/multi-keycloak/rabbitmq.conf) used on this setup)
- There are two OAuth2 clients (`prod_producer` and `dev_producer`) declared in keycloak and configured to access their respective audience: `rabbit_prod` and `rabbit_dev`
- RabbitMQ OAuth2 plugin has been configured with 2 resources: `rabbit_prod` and `rabbit_dev`
	```
	auth_oauth2.resource_servers.1.id = rabbit_prod
	auth_oauth2.resource_servers.2.id = rabbit_dev
	```
- Also RabbitMQ OAuth2 plugin has been configured with common settings for the two resources declared above
	```
	auth_oauth2.preferred_username_claims.1 = preferred_username
	auth_oauth2.preferred_username_claims.2 = user_name
	auth_oauth2.preferred_username_claims.3 = email
	auth_oauth2.jwks_url = https://keycloak:8443/realms/test/protocol/openid-connect/certs
	auth_oauth2.scope_prefix = rabbitmq.
	auth_oauth2.https.peer_verification = verify_peer
	auth_oauth2.https.cacertfile = /etc/rabbitmq/keycloak-cacert.pem
	```

Follow these steps to deploy Keycloak and RabbitMQ:

1. Launch Keycloak (http://localhost:8081/admin/master/console/#/test credentials `admin`:`admin`)
```
make start-keycloak
```
2. Launch RabbitMQ
```
export MODE="multi-keycloak"
make start-rabbitmq
```

3. Launch AMQP producer registered in Keycloak with the **client_id** `prod_producer` and with the permission to access `rabbit_prod` resource and with the scopes `rabbitmq.read:*/* rabbitmq.write:*/* rabbitmq.configure:*/*`:

```
make start-perftest-producer-with-token PRODUCER=prod_producer TOKEN=$(bin/keycloak/token prod_producer PdLHb1w8RH1oD5bpppgy8OF9G6QeRpL9)
```

This is an access token generated for `prod_producer`. The relevant attribute is `"aud": "rabbit_prod"`
```
{
  "exp": 1690974839,
  "iat": 1690974539,
  "jti": "c8edec50-5f29-4bd0-b25b-d7a46dc3474e",
  "iss": "http://localhost:8081/realms/test",
  "aud": "rabbit_prod",
  "sub": "826065e7-bb58-4b65-bbf7-8982d6cca6c8",
  "typ": "Bearer",
  "azp": "prod_producer",
  "acr": "1",
  "realm_access": {
    "roles": [
      "default-roles-test",
      "offline_access",
      "producer",
      "uma_authorization"
    ]
  },
  "resource_access": {
    "account": {
      "roles": [
        "manage-account",
        "manage-account-links",
        "view-profile"
      ]
    }
  },
  "scope": "profile email rabbitmq.read:*/* rabbitmq.write:*/* rabbitmq.configure:*/*",
  "clientId": "prod_producer",
  "clientHost": "172.18.0.1",
  "email_verified": false,
  "preferred_username": "service-account-prod_producer",
  "clientAddress": "172.18.0.1"
}
```

4. Similarly, launch AMQP producer `dev_producer`, registered in Keycloak too but with the permission to access `rabbit_dev` resource:
```
make start-perftest-producer-with-token PRODUCER=dev_producer TOKEN=$(bin/keycloak/token dev_producer z1PNm47wfWyulTnAaDOf1AggTy3MxX2H)
```
