# Expose multiple OAuth 2.0 resource(s)/audience(s) with one or many oauth providers

You are going to test the following OAuth flows:
1. Access AMQP protocol
2. Access Management UI and rest API

**NOTE** : This use case deploys a RabbitMQ docker image built from a PR which is still in progress.
The docker image is **pivotalrabbitmq/rabbitmq:create-oauth2-client-multi-resource-otp-max-bazel**

## Prerequisites to follow this guide

- Docker
- `/etc/hosts` must have the following entries. This is necessary if we want to access the management ui via the browser
```
127.0.0.1 localhost keycloak devkeycloak prodkeycloak
::1 localhost keycloak devkeycloak prodkeycloak
```

## Motivation

All the examples and use-cases demonstrated by this tutorial, except for this use case, configure a single **resource_server_id** and therefore a single **OAuth 2.0 server**.

The following three scenarios demonstrate how to configure two resources, called `rabbit_prod` and `rabbit_dev`:
- **Scenario 1** - Both resources are managed by the same **keycloak** realm and server. In other words, all users and clients are registered on the same OAuth 2.0 provider and realm within the provider
- **Scenario 2** - Each resource is managed on a dedicated realm (i.e. `rabbit_prod` resource -> `prod` realm, `rabbit_dev` resource -> `dev` realm) but under the same physical server, `keycloak`.
- **Scenario 3** - Each resource is managed on a dedicated OAuth server (i.e. `rabbit_dev` -> `devkeycloak:8442`, `rabbit_dev` -> `prodkeycloak:8443`).

## Scenario 1 - Two OAuth 2.0 resources under same OAuth provider and realm

In this scenario, we have the following OAuth clients declared on a single OAuth 2.0 provider called `keycloak` and realm `test`:
- Users and client with granted access to `rabbit_dev` resource:
	- `dev_producer` this is an OAuth client_id used by a producer application (password: `z1PNm47wfWyulTnAaDOf1AggTy3MxX2H`).
	- `rabbit_dev_admin` this is a management user which access RabbitMQ management ui.
	- `rabbit_dev_mgt_api` this is an OAuth client which accesses the management rest api with the `management` user-tag.
- Users and client with granted access to `rabbit_prod` resource:
	- `prod_producer` this is an OAuth client_id used by a producer application (password: `PdLHb1w8RH1oD5bpppgy8OF9G6QeRpL9`)
	- `rabbit_prod_admin` this is a management user

Follow these steps to deploy Keycloak and RabbitMQ:
1. Launch Keycloak
```
make start-keycloak
```
It is recommended to follow the logs until keycloak is fully initialized: `docker logs keycloak -f`

2. Launch RabbitMQ
```
MODE=keycloak CONF=rabbitmq.scenario1.conf make start-rabbitmq
```

3. Launch AMQP producer registered in Keycloak with the **client_id** `prod_producer` and with the permission to access `rabbit_prod` resource and with the scopes `rabbitmq.read:*/* rabbitmq.write:*/* rabbitmq.configure:*/*`:
```
make start-perftest-producer-with-token PRODUCER=prod_producer TOKEN=$(bin/keycloak/token prod_producer PdLHb1w8RH1oD5bpppgy8OF9G6QeRpL9)
```
4. Launch AMQP producer registered in Keycloak with the **client_id** `dev_producer` and with the permission to access `rabbit_dev` resource and with the scopes `rabbitmq.read:*/* rabbitmq.write:*/* rabbitmq.configure:*/*`:
```
make start-perftest-producer-with-token PRODUCER=dev_producer TOKEN=$(bin/keycloak/token dev_producer z1PNm47wfWyulTnAaDOf1AggTy3MxX2H)
```
5. Stop both producers
```
make stop-perftest-producer PRODUCER=dev_producer
make stop-perftest-producer PRODUCER=prod_producer
```
6. Verify `rabbit_dev_mgt_api` can access Management API because its token grants access to `rabbit_dev`
```
make curl-keycloak url=http://localhost:15672/api/overview client_id=rabbit_dev_mgt_api secret=jQa69T6KibxqrBokNTdFMroj3BN6H7dq
```
You should see in the standard output the json blob corresponding to the endpoint `/overview` in RabbitMQ's management api.

7. Verify `mgt_api_client` cannot access Management API because its token does not grant access to `rabbit_dev` or `rabbit_prod`
```
make curl-keycloak url=http://localhost:15672/api/overview client_id=mgt_api_client secret=LWOuYqJ8gjKg3D2U8CJZDuID3KiRZVDa
```
You should see in the standard output the following:
```
{"error":"not_authorized","reason":"Not_Authorized"}
```
8. Shutdown RabbitMq and Keycloak
```
make stop-keycloak
make stop-rabbitmq
```


## Scenario 2 - Two OAuth 2.0 resources on dedicated realm under the same many OAuth providers

In this scenario, we are still using the same single OAuth 2.0 provider called `keycloak`, but with the following setup:
- Under Realm `dev`:
	- `dev_producer` with the audience `rabbit_dev` (password: `z1PNm47wfWyulTnAaDOf1AggTy3MxX2H`)
	- `rabbit_dev_admin`
	- `rabbit_dev_mgt_api`
- Under Realm `prod`:
	- `prod_producer` with the audience `rabbit_prod` (password: `PdLHb1w8RH1oD5bpppgy8OF9G6QeRpL9`)
	- `rabbit_prod_admin` this is a management user which access RabbitMQ via the resource/audience `rabbit_prod`

In this scenarios, we have two OAuth resources declared in RabbitMQ, `rabbit_prod` and `rabbit_dev`. However, alike in scenario 1, users and clients are declared in two separate OAuth providers. A dedicated **keycloak** provider for each resource.

Follow these steps to deploy two Keycloaks and RabbitMQ:
1. Launch 2 Keycloaks
```
make start-dev-keycloak
make start-prod-keycloak
```
Run `docker ps | grep keycloak` to see the two instances.
It is recommended to follow the logs until both instances are fully initialized: `docker logs keycloak1 -f`

2. Launch RabbitMQ
```
MODE=multi-keycloak make start-rabbitmq
```
3. Launch AMQP producer registered in Keycloak with the **client_id** `prod_producer` and with the permission to access `rabbit_prod` resource and with the scopes `rabbitmq.read:*/* rabbitmq.write:*/* rabbitmq.configure:*/*`:
```
make start-perftest-producer-with-token PRODUCER=prod_producer TOKEN=$(bin/multi-keycloak/token prod_producer PdLHb1w8RH1oD5bpppgy8OF9G6QeRpL9 prod)
```
4. Launch AMQP producer registered in Keycloak with the **client_id** `dev_producer` and with the permission to access `rabbit_dev` resource and with the scopes `rabbitmq.read:*/* rabbitmq.write:*/* rabbitmq.configure:*/*`:
```
make start-perftest-producer-with-token PRODUCER=dev_producer TOKEN=$(bin/multi-keycloak/token dev_producer z1PNm47wfWyulTnAaDOf1AggTy3MxX2H dev)
```
5. Stop both producers
```
make stop-perftest-producer PRODUCER=dev_producer
make stop-perftest-producer PRODUCER=prod_producer
```
6. Verify `rabbit_dev_mgt_api` can access Management API because its token grants access to `rabbit_dev`
```
make curl-multi-keycloak url=http://localhost:15672/api/overview client_id=rabbit_dev_mgt_api secret=p7v6DksWkcb6TUYK6payswovC0LqhU6A keycloak=dev
```
You should see in the standard output the json blob corresponding to the endpoint `/overview` in RabbitMQ's management api.

7. Verify `mgt_api_client` cannot access Management API because its token does not grant access to `rabbit_dev` or `rabbit_prod`
```
make curl-multi-keycloak url=http://localhost:15672/api/overview client_id=mgt_api_client secret=LWOuYqJ8gjKg3D2U8CJZDuID3KiRZVDa keycloak=dev
```
You should see in the standard output the following:
```
{"error":"not_authorized","reason":"Not_Authorized"}
```
8. Verify the Management UI handles multiple resources.
	- Open http://localhost:15672 in the browser
	- Choose `rabbit_dev` resource
	- You should be redirected to `devkeycloak` to authenticate as `dev_user`/`dev_user`

8. Shutdown RabbitMq and the two Keycloaks
```
make stop-dev-keycloak
make stop-prod-keycloak
make stop-rabbitmq
```

## Scenario 3 - Two OAuth 2.0 resources on dedicated OAuth provider
