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
- **Scenario 1** - Both resources are managed by the same **keycloak** realm and server. In other words, all users and clients are registered on the same OAuth 2.0 provider and realm
- **Scenario 2** - Each resource is managed on a dedicated realm (i.e. `rabbit_prod` resource -> `https://keycloak:8443/realms/prod` realm, `rabbit_dev` resource -> `https://keycloak:8443/realms/dev`) but under the same physical server, `keycloak`.
- **Scenario 3** - Each resource is managed on a dedicated OAuth server and realm (i.e. `rabbit_dev` -> `https://devkeycloak:8443/realms/dev`, `rabbit_dev` -> `https://prodkeycloak:8442/realms/prod`).

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
MODE=multi-keycloak OAUTH_PROVIDER=keycloak CONF=rabbitmq.scenario1.conf make start-rabbitmq
```

3. Launch AMQP producer registered in Keycloak with the **client_id** `prod_producer` and with the permission to access `rabbit_prod` resource and with the scopes `rabbitmq.read:*/* rabbitmq.write:*/* rabbitmq.configure:*/*`:
```
make start-perftest-producer-with-token PRODUCER=prod_producer TOKEN=$(bin/keycloak/token prod_producer PdLHb1w8RH1oD5bpppgy8OF9G6QeRpL9 prod)
```
4. Launch AMQP producer registered in Keycloak with the **client_id** `dev_producer` and with the permission to access `rabbit_dev` resource and with the scopes `rabbitmq.read:*/* rabbitmq.write:*/* rabbitmq.configure:*/*`:
```
make start-perftest-producer-with-token PRODUCER=dev_producer TOKEN=$(bin/keycloak/token dev_producer z1PNm47wfWyulTnAaDOf1AggTy3MxX2H dev)
```
5. Stop both producers
```
make stop-perftest-producer PRODUCER=dev_producer
make stop-perftest-producer PRODUCER=prod_producer
```
6. Verify `rabbit_dev_mgt_api` can access Management API because its token grants access to `rabbit_dev`
```
make curl-keycloak url=http://localhost:15672/api/overview client_id=rabbit_dev_mgt_api secret=qcqIbJEDpwHTzimOrcD0FzJBj9C1pJsK realm=dev
```
You should see in the standard output the json blob corresponding to the endpoint `/overview` in RabbitMQ's management api.

7. Verify Management UI access using `rabbit_dev_admin` user (password: `rabbit_dev_admin`)

	- Go to http://localhost:15672
	- Click on "Click here to login"
	- Authenticate with Keycloak using `rabbit_dev_admin` / `rabbit_dev_admin`
	- Verify that user is redirected by to Management UI
	- Click on Logout
	- Repeat with user `rabbit_prod_admin` / `rabbit_prod_admin`

8. Verify `mgt_api_client` cannot access Management API because its token does not grant access to `rabbit_dev` or `rabbit_prod`
```
make curl-keycloak url=http://localhost:15672/api/overview client_id=mgt_api_client secret=LWOuYqJ8gjKg3D2U8CJZDuID3KiRZVDa realm=test
```
You should see in the standard output the following:
```
{"error":"not_authorized","reason":"Not_Authorized"}
```
9. Shutdown RabbitMq and Keycloak
```
make stop-keycloak
make stop-rabbitmq
```


## Scenario 2 - Two OAuth 2.0 resources on dedicated realm under the same many OAuth providers

In this scenario, we are still using the same single OAuth 2.0 provider called `keycloak`, but with the following setup:
- Under Realm `dev`:
	- `dev_producer` with the audience `rabbit_dev` (password: `SBuw1L5a7Y2aQfWfbsgXlEKGTNaEHxO8`)
	- `rabbit_dev_admin` (password: `rabbit_dev_admin`)
	- `rabbit_dev_mgt_api`
- Under Realm `prod`:
	- `prod_producer` with the audience `rabbit_prod` (password: `PdLHb1w8RH1oD5bpppgy8OF9G6QeRpL9`)
	- `rabbit_prod_admin` (password: `rabbit_prod_admin`)

In this scenarios, we have two OAuth resources declared in RabbitMQ, `rabbit_prod` and `rabbit_dev`. However, alike in scenario 1, users and clients are declared in two separate OAuth providers. A dedicated **keycloak** provider for each resource.

Follow these steps to deploy Keycloak and RabbitMQ:
1. Launch Keycloak
```
make start-keycloak
```
Run `docker ps | grep keycloak` to see the two instances.
It is recommended to follow the logs until both instances are fully initialized: `docker logs keycloak1 -f`

2. Launch RabbitMQ
```
MODE=multi-keycloak OAUTH_PROVIDER=keycloak CONF=rabbitmq.scenario2.conf make start-rabbitmq
```
3. Launch AMQP producer registered in Keycloak with the **client_id** `prod_producer` and with the permission to access `rabbit_prod` resource and with the scopes `rabbitmq.read:*/* rabbitmq.write:*/* rabbitmq.configure:*/*`:
```
make start-perftest-producer-with-token PRODUCER=prod_producer TOKEN=$(bin/keycloak/token prod_producer sIqZ5flmSz3r6uKXMSz8CWGeScdTpqq0 prod)
```
4. Launch AMQP producer registered in Keycloak with the **client_id** `dev_producer` and with the permission to access `rabbit_dev` resource and with the scopes `rabbitmq.read:*/* rabbitmq.write:*/* rabbitmq.configure:*/*`:
```
make start-perftest-producer-with-token PRODUCER=dev_producer TOKEN=$(bin/keycloak/token dev_producer SBuw1L5a7Y2aQfWfbsgXlEKGTNaEHxO8 dev)
```
5. Stop both producers
```
make stop-perftest-producer PRODUCER=dev_producer
make stop-perftest-producer PRODUCER=prod_producer
```
6. Verify `rabbit_dev_mgt_api` can access Management API because its token grants access to `rabbit_dev`
```
make curl-keycloak url=http://localhost:15672/api/overview client_id=rabbit_dev_mgt_api secret=La1Mvj7Qvt8iAqHisZyAguEE8rUpg014 realm=dev
```
You should see in the standard output the json blob corresponding to the endpoint `/overview` in RabbitMQ's management api.

8. Verify `mgt_api_client` cannot access Management API because its token does not grant access to `rabbit_dev` or `rabbit_prod`
```
make curl-keycloak url=http://localhost:15672/api/overview client_id=mgt_api_client secret=La1Mvj7Qvt8iAqHisZyAguEE8rUpg014 realm=test
```
You should see in the standard output the following:
```
{"error":"not_authorized","reason":"Not_Authorized"}
```

9. Verify Management UI access:

	- Go to http://localhost:15672
	- Click on "Click here to login"
	- Authenticate with Keycloak using `rabbit_dev_admin` / `rabbit_dev_admin`
	- Verify that user is redirected by to Management UI
	- Click on Logout
	- Repeat with user `rabbit_prod_admin` / `rabbit_prod_admin`

10. Shutdown RabbitMq and the two Keycloaks
```
make stop-keycloak
make stop-rabbitmq
```

## Scenario 3 - Two OAuth 2.0 resources on dedicated OAuth provider
