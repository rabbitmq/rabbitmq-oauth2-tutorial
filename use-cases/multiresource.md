# Expose multiple OAuth 2.0 resource(s)/audience(s) with one or many oauth providers

You are going to test the following OAuth flows:
1. Access AMQP protocol
2. Access Management UI and rest API

**NOTE** : This use case deploys a RabbitMQ docker image built from a PR which is still in progress.
The docker image is **pivotalrabbitmq/rabbitmq:create-oauth2-client-multi-resource-otp-max-bazel**

## Prerequisites to follow this guide

- Docker
- `/etc/hosts` must have the following entries. This is necessary if you want to access the management ui via the browser
```
127.0.0.1 localhost keycloak devkeycloak prodkeycloak
::1 localhost keycloak devkeycloak prodkeycloak
```

## Motivation

All the examples and use-cases demonstrated by this tutorial, except for this use case, configure a single **resource_server_id** and therefore a single **OAuth 2.0 server**.

The following three scenarios demonstrate how to configure two resources, called `rabbit_prod` and `rabbit_dev`:
- **Scenario 1** - Both resources are managed by the same **keycloak** realm and server. In other words, all users and clients are registered on the same OAuth 2.0 provider and realm
- **Scenario 1 with sligthly simpler configuration** - The scenario is exactly the same as in scenario 1 except with simpler configuration which makes some assumptions
- **Scenario 2** - Each resource is managed on a dedicated realm (i.e. `rabbit_prod` resource -> `https://keycloak:8443/realms/prod` realm, `rabbit_dev` resource -> `https://keycloak:8443/realms/dev`) but under the same physical server, `keycloak`.
- **Scenario 3** - Each resource is managed on a dedicated OAuth server and realm (i.e. `rabbit_dev` -> `https://devkeycloak:8443/realms/dev`, `rabbit_dev` -> `https://prodkeycloak:8442/realms/prod`).

## Scenario 1 - Two OAuth 2.0 resources under same OAuth provider and realm

This scenario uses a single OAuth 2.0 provider called `keycloak` and one realm called `test`:
- Users and client with granted access to `rabbit_dev` resource are:
	- `dev_producer` this is an OAuth client_id used by a producer application (password: `z1PNm47wfWyulTnAaDOf1AggTy3MxX2H`).
	- `rabbit_dev_admin` this is a management user which access RabbitMQ management ui.
	- `rabbit_dev_mgt_api` this is an OAuth client which accesses the management rest api with the `management` user-tag.
- Users and client with granted access to `rabbit_prod` resource are:
	- `prod_producer` this is an OAuth client_id used by a producer application (password: `PdLHb1w8RH1oD5bpppgy8OF9G6QeRpL9`)
	- `rabbit_prod_admin` this is a management user

Follow these steps to deploy Keycloak and RabbitMQ:
1. Launch Keycloak
```
make start-keycloak
```
It is recommended to follow the logs until keycloak is fully initialized: `docker logs keycloak -f`

2. Launch RabbitMQ with [rabbitmq.scenario1.conf](../conf/multi-keycloak/rabbitmq.scenario1.conf)
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

7. Verify Management UI access

	- Go to http://localhost:15672
	- Select *RabbitMQ Development* OAuth 2.0 resource
	- Click on "Click here to login"
	- Authenticate with Keycloak using `rabbit_dev_admin` / `rabbit_dev_admin`
	- Verify that user is redirected by to Management UI
	- Click on Logout
	- Repeat with *RabbitMQ Production* and user `rabbit_prod_admin` / `rabbit_prod_admin`

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

## Scenario 1 - Same setup but with simpler configuration

The [configuration](../conf/multi-keycloak/rabbitmq.scenario1.conf) used in previous section is the recommended configuration. It is more comprehensive to set all things about the OAuth provider into its own section as shown below although it certainly looks more verbose. First, you declare the OAuth provider's issuer url. If the TLS certificate has been signed by a root ca authority, most likely you don't need to need to set the `cacertfile`. However, here it is used a self-signed certificate hence it is needed.
```
auth_oauth2.oauth_providers.keycloak.issuer = https://keycloak:8443/realms/test
auth_oauth2.oauth_providers.keycloak.https.cacertfile = /etc/rabbitmq/keycloak-ca_certificate.pem
auth_oauth2.oauth_providers.keycloak.https.verify = verify_peer
auth_oauth2.oauth_providers.keycloak.https.hostname_verification = wildcard
```
And finally, you configure the provider as the default OAuth Provider. **If you miss this setting, OAuth 2.0 functionality is disabled**:
```
auth_oauth2.default_oauth_provider = keycloak
```

However, if you prefer to use the [configuration](../conf/multi-keycloak/rabbitmq.scenario1.basic.conf) used until RabbitMQ 3.12.x, here is the equivalent and used on this section.
```
# OAuth provider settings for all resources
auth_oauth2.issuer = https://keycloak:8443/realms/test
auth_oauth2.https.cacertfile = /etc/rabbitmq/keycloak-ca_certificate.pem
auth_oauth2.https.peer_verification = verify_peer
auth_oauth2.https.hostname_verification = wildcard
```

The other simplification introduced on this section is relative to the resource's label and the resource's scope.
Here is the configuration used in the previous section where each resource configured its label and scopes:
```
## Management ui settings for each declared resource server
management.oauth_resource_servers.1.id = rabbit_prod
management.oauth_resource_servers.1.oauth_client_id = rabbit_prod_mgt_ui
management.oauth_resource_servers.1.label = RabbitMQ Production
management.oauth_resource_servers.1.oauth_scopes = openid profile rabbitmq.tag:administrator

management.oauth_resource_servers.2.id = rabbit_dev
management.oauth_resource_servers.2.oauth_client_id = rabbit_dev_mgt_ui
management.oauth_resource_servers.2.label = RabbitMQ Development
management.oauth_resource_servers.2.oauth_scopes = openid profile rabbitmq.tag:management
```

Whereas in this section's configuration, there is no label hence the label is the resource's id. This is perfectly valid. However, if the resource's id is not very user-friendly, it is preferable to set a label.
Also, in this section's configuration, both resources claim the same scopes therefore the scopes are set up at the root level.
```
management.oauth_scopes = openid profile rabbitmq.tag:administrator rabbitmq.tag:management

## Management ui settings for each declared resource server
management.oauth_resource_servers.1.id = rabbit_prod
management.oauth_resource_servers.1.oauth_client_id = rabbit_prod_mgt_ui

management.oauth_resource_servers.2.id = rabbit_dev
management.oauth_resource_servers.2.oauth_client_id = rabbit_dev_mgt_ui
```

To test this scenario follow the steps used in previous scenario, except for step 2, which
launches RabbitMQ. Instead use the following command:
```
MODE=multi-keycloak OAUTH_PROVIDER=keycloak CONF=rabbitmq.scenario1.basic.conf make start-rabbitmq
```

When you go to the management ui (http://localhost:15672/), you will see the options in the list of OAuth 2.0 resources correspond to the list of resources' id.


## Scenario 2 - Two OAuth 2.0 resources on dedicated realm under the same many OAuth providers

This scenario is still using the same single OAuth 2.0 provider called `keycloak`, however, this time there are two realms, `dev` and `prod`, and each realm but with the following setup:
- Under Realm `dev` there are users and clients with granted access to the `rabbit_dev` resource:
	- `dev_producer` (password: `SBuw1L5a7Y2aQfWfbsgXlEKGTNaEHxO8`)
	- `rabbit_dev_admin` (password: `rabbit_dev_admin`)
	- `rabbit_dev_mgt_api`
- Under Realm `prod` there are users and clients with granted access to the `rabbit_prod` resource:
	- `prod_producer` with the audience `rabbit_prod` (password: `PdLHb1w8RH1oD5bpppgy8OF9G6QeRpL9`)
	- `rabbit_prod_admin` (password: `rabbit_prod_admin`)

Alike in the previous scenario, we configure RabbitMQ with two OAuth 2.0 providers when there is really one.
However, this provider is multitenant and each tenant acts like a separate OAuth provider with its own issuer URL. Check out the section `oauth_providers` in [RabbitMQ Configuration](../conf/multi-keycloak/rabbitmq.scenario2.conf) used by this scenario. For convenience here is the relevant part:
```
...
## Oauth providers
auth_oauth2.oauth_providers.devkeycloak.issuer = https://keycloak:8443/realms/dev
auth_oauth2.oauth_providers.devkeycloak.https.cacertfile = /etc/rabbitmq/keycloak-ca_certificate.pem
auth_oauth2.oauth_providers.devkeycloak.https.verify = verify_peer
auth_oauth2.oauth_providers.devkeycloak.https.hostname_verification = wildcard

auth_oauth2.oauth_providers.prodkeycloak.issuer = https://keycloak:8443/realms/prod
auth_oauth2.oauth_providers.prodkeycloak.https.cacertfile = /etc/rabbitmq/keycloak-ca_certificate.pem
auth_oauth2.oauth_providers.prodkeycloak.https.verify = verify_peer
auth_oauth2.oauth_providers.prodkeycloak.https.hostname_verification = wildcard

...
```

Follow these steps to deploy Keycloak and RabbitMQ:
1. Launch Keycloak
```
make start-keycloak
```
Run `docker ps | grep keycloak` to check instance has started
It is recommended to follow the logs until both instances are fully initialized: `docker logs keycloak -f`

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

9. Verify Management UI access

	- Go to http://localhost:15672
	- Select *RabbitMQ Development* OAuth 2.0 resource
	- Click on "Click here to login"
	- Authenticate with Keycloak using `rabbit_dev_admin` / `rabbit_dev_admin`
	- Verify that user is redirected by to Management UI
	- Click on Logout
	- Repeat with *RabbitMQ Production* and user `rabbit_prod_admin` / `rabbit_prod_admin`

10. Shutdown RabbitMq and the two Keycloaks
```
make stop-keycloak
make stop-rabbitmq
```

## Scenario 3 - Two OAuth 2.0 resources on dedicated OAuth provider

This scenario uses two separate OAuth 2.0 provider called `devkeycloak` and `prodkeycloak`, with the following setup:
- `devkeycloak` has the following setup under the `dev` Realm and grants access to `rabbit_dev` resource:
	- `dev_producer` with the audience `rabbit_dev` (password: `SBuw1L5a7Y2aQfWfbsgXlEKGTNaEHxO8`)
	- `rabbit_dev_admin` (password: `rabbit_dev_admin`)
	- `rabbit_dev_mgt_api`
- `prodkeycloak` has the following setup under the `prod` Realm and grants access to `rabbit_prod` resource:
	- `prod_producer` with the audience `rabbit_prod` (password: `PdLHb1w8RH1oD5bpppgy8OF9G6QeRpL9`)
	- `rabbit_prod_admin` (password: `rabbit_prod_admin`)

Check out the section `oauth_providers` in [RabbitMQ Configuration](../conf/multi-keycloak/rabbitmq.scenario3.conf) used by this scenario. Like in the scenario 2, there are two OAuth providers however this time the URL refers to two different hostnames. For convenience here is the relevant part:
```
...

## Oauth providers
auth_oauth2.oauth_providers.devkeycloak.issuer = https://devkeycloak:8443/realms/dev
auth_oauth2.oauth_providers.devkeycloak.https.cacertfile = /etc/rabbitmq/keycloak-ca_certificate.pem
auth_oauth2.oauth_providers.devkeycloak.https.verify = verify_peer
auth_oauth2.oauth_providers.devkeycloak.https.hostname_verification = wildcard

auth_oauth2.oauth_providers.prodkeycloak.issuer = https://prodkeycloak:8442/realms/prod
auth_oauth2.oauth_providers.prodkeycloak.https.cacertfile = /etc/rabbitmq/keycloak-ca_certificate.pem
auth_oauth2.oauth_providers.prodkeycloak.https.verify = verify_peer
auth_oauth2.oauth_providers.prodkeycloak.https.hostname_verification = wildcard

...
```

Follow these steps to deploy two Keycloaks and RabbitMQ:
1. Launch Keycloak
```
make start-dev-keycloak
make start-prod-keycloak
```
Run `docker ps | grep keycloak` to see two instances have started.

2. Launch RabbitMQ
```
MODE=multi-keycloak CONF=rabbitmq.scenario3.conf make start-rabbitmq
```
3. Launch AMQP producer registered in Keycloak with the **client_id** `prod_producer` and with the permission to access `rabbit_prod` resource and with the scopes `rabbitmq.read:*/* rabbitmq.write:*/* rabbitmq.configure:*/*`:
```
make start-perftest-producer-with-token PRODUCER=prod_producer TOKEN=$(bin/prodkeycloak/token prod_producer PdLHb1w8RH1oD5bpppgy8OF9G6QeRpL9)
```
4. Launch AMQP producer registered in Keycloak with the **client_id** `dev_producer` and with the permission to access `rabbit_dev` resource and with the scopes `rabbitmq.read:*/* rabbitmq.write:*/* rabbitmq.configure:*/*`:
```
make start-perftest-producer-with-token PRODUCER=dev_producer TOKEN=$(bin/devkeycloak/token dev_producer z1PNm47wfWyulTnAaDOf1AggTy3MxX2H)
```
5. Stop both producers
```
make stop-perftest-producer PRODUCER=dev_producer
make stop-perftest-producer PRODUCER=prod_producer
```
6. Verify `rabbit_dev_mgt_api` can access Management API because its token grants access to `rabbit_dev`
```
make curl-dev-keycloak url=http://localhost:15672/api/overview client_id=rabbit_dev_mgt_api secret=p7v6DksWkcb6TUYK6payswovC0LqhU6A
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

9. Verify Management UI access

	- Go to http://localhost:15672
	- Select *RabbitMQ Development* OAuth 2.0 resource
	- Click on "Click here to login"
	- Authenticate with Keycloak using `rabbit_dev_admin` / `rabbit_dev_admin`
	- Verify that user is redirected by to Management UI
	- Click on Logout
	- Repeat with *RabbitMQ Production* and user `rabbit_prod_admin` / `rabbit_prod_admin`

10. Shutdown RabbitMq and the two Keycloaks
```
make stop-dev-keycloak
make stop-prod-keycloak
make stop-rabbitmq
```
