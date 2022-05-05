# RabbitMQ OAuth2 Tutorial

These are the two goals of this guide:
1. Explore how applications and end users can authenticate with RabbitMQ server using OAuth 2.0 protocol rather than the traditional username/password, or others.
2. Explore what it takes to set up RabbitMQ Server with OAuth 2.0 authentication mechanism. Additionally we explore how to stand up ([UAA](https://github.com/cloudfoundry/uaa)) as an OAuth 2.0 Authorization Server and all the operations to create OAuth clients, users and obtain their tokens.

If you want to quickly test how it works go straight to [OAuth2 plugin in action](#oauth2-plugin-in-action) section.

If you want to understand the details of how to configure RabbitMQ with Oauth2 go straight to [Understand the environment](#understand-the-environment) section.

**Table of Content**

<!-- TOC depthFrom:2 depthTo:3 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Prerequisites to follow this guide](#prerequisites-to-follow-this-guide)
- [OAuth2 plugin in action](#oauth2-plugin-in-action)
	- [Set up UAA and RabbitMQ](#set-up-uaa-and-rabbitmq)
	- [Use Case 1 Management user accessing the Management UI](#use-case-1-management-user-accessing-the-management-ui)
	- [Use Case 2 Monitoring agent accessing management REST api](#use-case-2-monitoring-agent-accessing-management-rest-api)
	- [Use Case 3 AMQP protocol](#use-case-3-amqp-protocol)
	- [Use Case 4 JMS protocol](#use-case-4-jms-protocol)
	- [Use Case 5 Use custom scope field](#use-case-5-use-custom-scope-field)
	- [Use Case 6 Use multiple asymmetrical signing keys](#use-case-6-use-multiple-asymmetrical-signing-keys)
	- [Use Case 7 MQTT protocol](#use-case-7-mqtt-protocol)
	- [Use Case 8 Use external OAuth server https://auth0.com/](#use-case-8-use-external-oauth-server-httpsauth0com)
	- [Use Case 9 Use custom scopes](#use-case-9-use-custom-scopes)
- [Understand the environment](#understand-the-environment)
	- [RabbitMQ server](#rabbitmq-server)
	- [UAA server](#uaa-server)
	- [UAA client](#uaa-client)
	- [Clients, Users & Permissions in UAA](#clients-users-permissions-in-uaa)
- [Understand a bit more about OAuth in the context of RabbitMQ](#understand-a-bit-more-about-oauth-in-the-context-of-rabbitmq)
	- [About Users and Clients](#about-users-and-clients)
	- [About Permissions](#about-permissions)
	- [About signing key required to configure RabbitMQ](#about-signing-key-required-to-configure-rabbitmq)
	- [About rotating UAA signing key](#about-rotating-uaa-signing-key)
	- [Understanding how an AMQP application access RabbitMQ using Oauth2](#understanding-how-an-amqp-application-access-rabbitmq-using-oauth2)
	- [AMQP access via Spring and Spring Cloud Services using OAuth Client Credentials grant type](#amqp-access-via-spring-and-spring-cloud-services-using-oauth-client-credentials-grant-type)
	- [Understanding Access tokens and how RabbitMQ uses it](#understanding-access-tokens-and-how-rabbitmq-uses-it)
	- [Useful uaac commands](#useful-uaac-commands)
- [Findings](#findings)
	- [Management UI shows token's client_id as user in the top right corner](#management-ui-shows-tokens-clientid-as-user-in-the-top-right-corner)
	- [Logout from management ui should redirect the user back to the initial login](#logout-from-management-ui-should-redirect-the-user-back-to-the-initial-login)

<!-- /TOC -->

## Prerequisites to follow this guide

- Docker must be installed
- Ruby must be installed
- make


## OAuth2 plugin in action

First of all, let's bring up RabbitMQ fully configured with Oauth2 plugin and UAA as an OAuth2 Authorization Server.
> RabbitMQ has to be configured with facts about the Authorization server hence the arrow from RabbitMQ to UAA.

```
    [ UAA ]    <-------------    [ RabbitMQ ]

```

### Set up UAA and RabbitMQ

There are two ways to set up OAuth2 in RabbitMQ. One uses symmetrical signing keys. And the other uses
asymmetrical signing keys. The Authorization server is who digitally signs the JWT tokens and RabbitMQ
has to be configured to validate any of the two types of digital signatures.

Given that asymmetrical keys is the most widely used option, we are going to focus on how to
configure RabbitMQ with them.

#### Use Asymmetrical digital singing keys

Run the following 4 commands to get the environment ready to see Oauth2 plugin in action:

  1. Build UAA docker image if you have not done it yet (see instructions in the previous section)  
	   `make build-uaa`
  2. `make start-uaa` to get UAA server running
  3. `make setup-users-and-clients` to install uaac client; connect to UAA server and set ups users, group, clients and permissions
		> *IMPORTANT*: hit enter when prompted for client secret.

  4. `make start-rabbitmq` to start RabbitMQ server


### Use Case 1 Management user accessing the Management UI

The first time an end user arrives to the management ui (`1`), they are redirected (`2`) to UAA to authenticate. Once they successfully authenticate with UAA, the user is redirected back (`3.`) to RabbitMQ with a valid JWT token. RabbitMQ validates it and identify the user and its permissions from the JWT token.

```
    [ UAA ] <----2. auth----    [ RabbitMQ ]
            ----3. redirect-->  [  http    ]
                                  /|\
                                   |
                            1. rabbit_admin from a browser
```

> At step 2, if this is the first time the user is accessing RabbitMQ resource, UAA will prompt the user to
authorize RabbitMQ application as shown on the screenshot below.
> ![authorize application](assets/authorize-app.png)

We have previously configured UAA with 2 users:
 - `rabbit_admin:rabbit_admin`
 - and `rabbit_monitor:rabbit_monitor`

Go to http://localhost:15672 and login using any of those two users.

> TL;DR the user displayed by the management ui is not the user name but `rabbitmq_client` which is the
identity of RabbitMQ to work on half of the user

This is a token issued by UAA for the `rabbit_admin` user thru the redirect flow we just saw above.
It was signed with the symmetric key.

![JWT token](assets/admin-token-signed-sym-key.png)

### Use Case 2 Monitoring agent accessing management REST api

We may have a monitoring agent such as Prometheus accessing RabbitMQ management REST api; or other type of agent checking the health of RabbitMQ. Because it is not an end user, or human, we refer to it as a *service account*. This *service account* could be our `mgt_api_client` client we created in UAA with the `monitoring` *user tag*.

This *monitoring agent* would use the *client credentials* or *password* grant flow to authenticate (`1`) with
UAA and get back a JWT token (`2.`). Once it gets the token, it sends (`3.`) a HTTP request to the RabbitMQ management endpoint passing the JWT token.

```
    [ UAA ]                  [ RabbitMQ ]
      /|\                    [  http    ]
       |                          /|\
       |                       3.http://broker:15672/api/overview passing JWT token
       |                           |
       +-----1.auth---------  monitoring agent
       --------2.JWT-------->
```

The following command launches the browser with `mgt_api_client` client with a JWT token previously obtained from UAA:
```
make curl url=http://localhost:15672/api/overview client_id=mgt_api_client secret=mgt_api_client
```


### Use Case 3 AMQP Protocol

**DL;DR:**
  In this section, we are demonstrating how an application can connect to RabbitMQ presenting a JWT Token as a credential. The application we are going to use is [PerfTest](https://github.com/rabbitmq/rabbitmq-perf-test) which is not an OAuth 2.0 aware application -see [next use case](#) for an OAuth 2.0 aware application.

  Instead we are launching the application with a token that we have previously obtained from UAA. This is just to probe AMQP access with a JWT Token. Needless to say that the application should instead obtain the JWT Token prior to connecting to RabbitMQ and it should also be able to refresh it before reconnecting. RabbitMQ validates the token before accepting it. If the token has expired, RabbitMQ will reject the connection.


First of all, an application which wants to connect to RabbitMQ using Oauth2 must present a
valid JWT token. To obtain the token, the application must first authenticate (`1.`) with UAA. In case of a successful
authentication, it gets back a JWT token (`2.`) which uses it to connect (`3.`) to RabbitMQ.  


```
    [ UAA ]                  [ RabbitMQ ]
      /|\                    [  amqp    ]
       |                          /|\
       |                       3.connect passing JWT
       |                           |
       +-----1.auth---------  amqp application
       --------2.JWT-------->
```

We have previously configured UAA with these 2 OAuth clients:
 - `consumer`
 - and `producer`
> An application requires an oauth client in order to get an JWT token. Applications use the `Oauth client grant flow` to obtain a JWT token

This the token issued by UAA for the `consumer` OAuth client.

![JWT token](assets/consumer-token-signed-with-sym-key.png)

To launch the consumer application invoke the following command:
```
make start-perftest-consumer
```
> To check the logs : docker logs consumer -f

To launch the producer application invoke the following command:
```
make start-perftest-producer
```
> To check the logs : docker logs producer -f


To stop all the applications call the following command:
```
make stop-all-apps
```


### Use Case 4 JMS protocol

In this use case we are demonstrating a basic JMS application which reads, via an environment variable (`TOKEN`),
the JWT token that will use as password when authenticating with RabbitMQ.

It is **VERY IMPORTANT** to grant the required permission to the *exchange* `jms.durable.queues`.

Applications which send JMS messages require of these permissions:
- `rabbitmq.configure:*/jms.durable.queues`
- `rabbitmq.write:*/jms.durable.queues`
- `rabbitmq.read:*/jms.durable.queues`
> Those permissions grant access on any vhost.

Before testing a publisher and a subscriber application we need to build a local image for the
basic jms application by invoking this command:
```
make build-jms-client
```

To test a JMS application sending a message and authenticating via OAuth run this command:
```
make start-jms-publisher
```
> It sends a message to a queue called `q-test-queue`

Applications which subscribe to a JMS queue require of these permissions:
- `rabbitmq.write:*/jms.durable.queues`
> Those permissions grant access on any vhost.

To test a JMS application subscribing to a queue and authenticating via OAuth run this command:
```
make start-jms-subscriber
```
> It subscribes to a queue called `q-test-queue`

### Use Case 5 Use custom scope field  

There are some Authorization servers which cannot include RabbitMQ scopes into the standard
JWT `scope` field. Instead, they can include RabbitMQ scopes in a custom JWT scope of their choice.

Since RabbitMQ 3.9, it is possible to configure RabbitMQ with a different field to look for scopes as shown below:

```
[
  {rabbitmq_auth_backend_oauth2, [
    ...
    {extra_scopes_source, <<"extra_scope">>},
    ...
    ]}
  ]},
].
```

To test this feature we are going to build a token, sign it and use it to hit one of the RabbitMQ management endpoints.
The command below allows us to hit any management endpoint, in this case it is the `overview`, with a token.

```
make curl-with-token URL=http://localhost:15672/api/overview TOKEN=$(bin/jwt_token scope-and-extra-scope.json legacy-token-key private.pem public.pem)
```


We use the python script `bin/jwt_token.py` to build the minimal JWT token possible that RabbitMQ is able to
validate which is:
```
{
  "scope": [

  ],
  "extra_scope": [
    "rabbitmq.tag:management"
  ],
  "aud": [
    "rabbitmq"
  ]
}
```

### Use Case 6 Use multiple asymmetrical signing keys

This scenario explores the use case where JWT tokens may be signed by different asymmetrical signing keys.

There are 2 ways to configure RabbitMQ with multiple signing keys:
- We can either **statically** configure them via `rabbitmq.conf` as shown in the [plugin documentation page](https://github.com/rabbitmq/rabbitmq-server/tree/master/deps/rabbitmq_auth_backend_oauth2#variables-configurable-in-rabbitmqconf).
- Or we can do it **dynamically**, i.e, add signing keys while RabbitMQ is running and without having to
restart it. This alternative is explained in more detail in the section [About rotating UAA signing key](#about-rotating-uaa-signing-key).
However, we are going to demonstrate it here as well.

First we add a second signing key called `legacy-token-2-key` whose public key is `conf/public-2.pem`:
```
docker exec -it rabbitmq rabbitmqctl add_uaa_key legacy-token-2-key --pem-file=/conf/public-2.pem
Adding UAA signing key "legacy-token-2-key" filename: "/conf/public-2.pem"
```

And then we issue a token using the corresponding private key and use it to access the management endpoint `/api/overview`.

```
make curl-with-token URL=http://localhost:15672/api/overview TOKEN=$(bin/jwt_token scope-and-extra-scope.json legacy-token-2-key private-2.pem public-2.pem)
```
> jwt_token searches for private and public key files under `conf` folder and jwt files under `jwts`.

### Use Case 7 MQTT protocol

This scenario explores the use case where we authenticate with a JWT token to RabbitMQ MQTT port.

> Note: RabbitMQ is already configured with `rabbitmq_mqtt` plugin.

This is no different than using AMQP or JMS protocols, all that matters is to pass an empty username and a JWT token as password.
However, **what it is really different** is how we encode the permissions. In this use case we are going to proceed as we did it in the previous use case where we handcrafted the JWT token rather than requesting it to UAA. Here is the the scopes required to publish
a message to a mqtt topic ([scopes-for-mqtt.json](jwts/scopes-for-mqtt.json))
```
{
  "scope": [
    "rabbitmq.write:*/*/*",
    "rabbitmq.configure:*/*/*",
    "rabbitmq.read:*/*/*"

  ],
  "extra_scope": "rabbitmq.tag:management",
  "aud": [
    "rabbitmq"
  ]
}
```

`rabbitmq.write:*/*/*` means allow write operation on a any vhost, on any exchange and any topic. In fact,
it is any "routing-key" because that is translated to a topic/queue.

We are going to publish a mqtt message by running the following command. If you have not run any of the
previous use cases, you need to launch rabbitmq first like this `make start-uaa`.
```
make start-mqtt-publish TOKEN=$(bin/jwt_token scopes-for-mqtt.json legacy-token-key private.pem public.pem)
```

> IMPORTANT: If you try to access the management ui and authenticate with UAA using rabbit_admin you
wont be able to do bind a queue with routing_key `test` to the `amq.topic` exchange because that user
in UAA does not have the required permissions. In our handcrafted token, we have granted ourselves the right permissions/scopes.

### Use Case 8 Use external OAuth server https://auth0.com/

In order to follow this use case, we must first sign up for an account in https://auth0.com/.

We are going to test 2 OAuth flows:
1. An Oauth client/application access the management rest api or one of the messaging protocols like AMQP
2. An Oauth user, via a browser, comes to the management ui

To test the first flow, we need to follow these steps:
1. Once you have logged onto your account in https://auth0.com/, go to dashboard > Applications > APIs > Create an API
2. Give it the name `rabbitmq`. The important thing here is the `identifier` which must have the name of the *resource_server_id* we configured in RabbitMQ. This `identifier` goes into the `audience` JWT field. In our case, it is called `rabbitmq`. And we choose `RS256` as the signing algorithm.
3. Edit the API we just created with the name `rabbitmq`. Go into Permissions and add the permissions (scope) this api can grant
4. For every API we create, an *Application* gets created using the API's `identifier` as its name.
5. Go to dashboard > Applications, and you should see your application listed.
6. An application gives us a *client_id*, a *client_secret* and a http endpoint called *Domain* where to claim a token. An Application represents an *OAuth Client**
5. Go into dashboard > Applications > rabbitmq > APIs, you will see a list of all the APIs including the one we just created. Along with each API there is a toggle to authorize the Application to use the API. Once you "authorize" the Application to use an API, you can pick which scopes you want to grant to the Application from the list of scopes allowed by the API.

We are done setting things up in Oauth0, now we can claim a token like this:
```
	curl curl --request POST \
  --url 'https://<copy the domain from the Application settings>/oauth/token' \
  --header 'content-type: application/x-www-form-urlencoded' \
  --data grant_type=client_credentials \
  --data client_id=<copy the client ID field from the Application settings> \
  --data client_secret=<copy the client secret field from the Application settings> \
  --data audience="<copy the identifier field from the API settings>"

```



### Use Case 9 Use custom scopes

In this use case we are going to demonstrate how to configure RabbitMQ to handle
*custom scopes*. But what are *custom scopes*? They are any
scope whose format is not compliant with RabbitMQ format. For instance, `api://rabbitmq:Read.All`
is one of the custom scopes we will use in this use case.

#### How to configure RabbitMQ with custom scope mapping

Since RabbitMQ `3.10.0-rc.6`, we are able to map a custom scope to one or many RabbitMQ scopes.
See below a sample RabbitMQ configuration where we map `api://rabbitmq:Read.All`
custom scope to `rabbitmq.read:*/*` RabbitMQ scope.
```
{rabbitmq_auth_backend_oauth2, [
 ...,
	{scope_aliases, #{
		<<"api://rabbitmq:Read.All">>      => [<<"rabbitmq.read:*/*">>],
	  ...
	},
	...
]}
```

Additionally, we can map a custom scope to many RabbitMQ scopes. For instance below we
are mapping the role `api://rabbitmq:producer` to 3 RabbitMQ scopes which grants
`read`, `write` and `configure` access on any resource and on any vhost:
```
{rabbitmq_auth_backend_oauth2, [
 ...,

	{scope_aliases, #{
		<<"api://rabbitmq:producer">> => [
			<<"rabbitmq.read:*/*">>,
			<<"rabbitmq.write:*/*">>,
			<<"rabbitmq.configure:*/*">>
		]
	}},
	...
]}
```

#### How custom scopes are carried in JWT tokens

If we do not configure RabbitMQ OAuth2 plugin with `extra_scopes_source`, RabbitMQ
expects the `scope` token's field to carry *custom scopes*. For instance, below we have a sample JWT
token where the custom scopes are in the `scope` field :
```
{
  "sub": "producer",
  "scope": [
    "api://rabbitmq:producer",
    "api://rabbitmq:Administrator"
  ],
  "aud": [
    "rabbitmq"
  ]
}
```

Now, let's say we do configure RabbitMQ OAuth2 plugin with `extra_scopes_source` as shown below:
```
  {rabbitmq_auth_backend_oauth2, [
    {resource_server_id, <<"rabbitmq">>},
    {extra_scopes_source, <<"roles">>},
    ...
```

With this configuration, RabbitMQ expects *custom scopes* in the field `roles` and
the `scope` field is ignored.
```
{
  "sub": "rabbitmq-client-code",
  "roles": "api://rabbitmq:Administrator.All",
  "aud": [
    "rabbitmq"
  ]
}
```

#### UAA configuration

To demonstrate this new capability we have configured UAA with two Oauth2 clients. One
called `producer_with_roles` with the *custom scope* `api://rabbitmq:producer` and `consumer_with_roles` with
`api://rabbitmq:Read:All,api://rabbitmq:Configure:All,api://rabbitmq:Write:All`.
> we are granting configure and write permissions to the consumer because we have configured perf-test to declare
resources regardless whether it is a producer or consumer application.

These two uaac commands declare the two oauth2 clients above. We are adding an extra scope called `rabbitmq.*` so
that UAA populates the JWT claim `aud` with the value `rabbitmq`. RabbitMQ expects `aud` to match the value we
configure RabbitMQ with in the `resource_server_id` field.

```
uaac client add producer_with_roles --name producer_with_roles \
    --authorities "rabbitmq.*,api://rabbitmq:producer,api://rabbitmq:Administrator" \
    --authorized_grant_types client_credentials \
    --secret producer_with_roles_secret
uaac client add consumer_with_roles --name consumer_with_roles \
    --authorities "rabbitmq.* api://rabbitmq:read:All" \
    --authorized_grant_types client_credentials \
    --secret consumer_with_roles_secret
```


#### RabbitMQ configuration

There are two configuration files ready to use to launch RabbitMQ:
- [conf/asymmetric_key/rabbitmq-scope-aliases.config](conf/asymmetric_key/rabbitmq-scope-aliases.config) - which configures scope mappings.
- [conf/asymmetric_key/rabbitmq-scope-aliases-and-extra-scope.config](conf/asymmetric_key/rabbitmq-scope-aliases-and-extra-scope.config) - which configures `extra_scopes_source` and scope mappings.


#### Demo 1 - Launch RabbitMQ with custom scopes in scope field

To launch RabbitMq with scope mappings and with *custom scopes* in the `scope` field we run the following command:
```
CONFIG=rabbitmq-scope-aliases.config make start-rabbitmq
```
> This command will stop RabbitMQ if it is already running


Launch a producer application with the client `producer_with_roles`
```
make start-perftest-producer PRODUCER=producer_with_roles
```
> To check the logs : docker logs producer_with_roles -f  

Launch a consumer application with the client `consumer_with_roles`
```
make start-perftest-consumer CONSUMER=consumer_with_roles
```
> To check the logs : docker logs consumer_with_roles -f  

Access management api with the client `producer_with_roles`
```
make curl url=http://localhost:15672/api/overview client_id=producer_with_roles secret=producer_with_roles_secret
```

To stop the perf-test applications run :
```
make stop-perftest-producer PRODUCER=producer_with_roles
make stop-perftest-consumer CONSUMER=consumer_with_roles
```

#### Demo 2 - Launch RabbitMQ with custom scopes in extra scope field

To launch RabbitMq with scope mappings and with *custom scopes* in the `extra_scope` we run the following command:
```
CONFIG=rabbitmq-scope-aliases-and-extra-scope.config make start-rabbitmq
```
> This command will stop RabbitMQ if it is already running

We cannot use UAA to issue the tokens because we cannot configure UAA to use a custom field for scopes.
Instead we are going to issue the token ourselves with the command `bin/jwt_token`.

Launch a producer application with the token [producer-role-in-scope.json](jwts/producer-roles-in-extra-scope.json):
```
make start-perftest-producer-with-token PRODUCER=producer_with_roles TOKEN=$(bin/jwt_token producer-role-in-extra-scope.json legacy-token-key private.pem public.pem)
```
> To check the logs :  docker logs producer_with_roles -f

Launch a consumer application with the token [consumer-roles-in-extra-scope.json](jwts/consumer-roles-in-extra-scope.json):
```
make start-perftest-consumer-with-token CONSUMER=consumer_with_roles TOKEN=$(bin/jwt_token consumer-roles-in-extra-scope.json legacy-token-key private.pem public.pem)
```

Access management api with the token [producer-roles-in-extra-scope.json](jwts/producer-roles-in-extra-scope.json)
```
make curl-with-token URL="http://localhost:15672/api/overview" TOKEN=$(bin/jwt_token producer-roles-in-extra-scope.json legacy-token-key private.pem public.pem)
```

To stop the perf-test applications run :
```
make stop-perftest-producer PRODUCER=producer_with_roles
make stop-perftest-consumer CONSUMER=consumer_with_roles
```


## Understand the environment

### RabbitMQ server

We need to launch RabbitMQ with the following prerequisites:
- plugin enabled. See [conf/enabled_plugins](conf/enabled_plugins)
- plugin configured with the signing key used by UAA. For more details check out [this section](#about-signing-key-required-to-configure-rabbitmq)
  ```
    {rabbitmq_auth_backend_oauth2, [
      {resource_server_id, <<"rabbitmq">>}
      {key_config, [
        {default_key, <<"legacy-token-key">>},
        {signing_keys, #{
          <<"legacy-token-key">> => {map, #{<<"kty">> => <<"MAC">>,
                                    <<"alg">> => <<"HS256">>,
                                    <<"use">> => <<"sig">>,
                                    <<"value">> => <<"tokenKey">>}}
        }}
      ]}
    ]},
  ```
- rabbit configured with Oauth2 auth-backend and internal auth-backend
```
[
  % Enable auth backend
  {rabbit, [
     {auth_backends, [rabbit_auth_backend_oauth2, rabbit_auth_backend_internal]}
  ]},
].
```
- rabbit management plugin configured with UAA. This includes the auto client (`rabbit_client`) RabbitMQ uses to
authenticate users with UAA and the URL of UAA (`http://localhost:8080/uaa`)
```
[
  {rabbitmq_management, [
       {enable_uaa, true},
       {uaa_client_id, "rabbit_client"},
       {uaa_location, "http://localhost:8080/uaa"}
  ]},
].
```
- check out the full [conf/rabbitmq.config.symmetrical](conf/rabbitmq.config.symmetrical)


### UAA server

Standalone OAuth2 server (https://github.com/cloudfoundry/uaa). Its primary role is as an OAuth2 provider, issuing tokens for client applications to use when they act on behalf of Cloud Foundry users. It can also authenticate users with their Cloud Foundry credentials, and can act as an SSO service using those credentials. It has endpoints for managing user accounts and for registering OAuth2 clients, as well as various other management functions

**IMPORTANT**:
- UAA can run with an external database. But for the purposes of this exploration, the internal database is sufficient

To check that UAA is running fine:
```
curl -k  -H 'Accept: application/json' http://localhost:8080/uaa/info | jq .
```

Currently RabbitMQ Management plugin does not support latest version of UAA. That is
why in order to run the use cases we use the image built from the folder `uaa-4.24`. This has to do
with the javascript library that comes with the management plugin.


### UAA client

In order to interact with UAA server there is a convenient command-line application called `uaac`. To install it and get it ready run the following command:
```
make install-uaac
```

> In order to operate with uaa we need to "authenticate". There is an OAuth client preconfigured with the following credentials `admin:adminsecret`. This user is configured under <uaa_repo>/uaa/src/main/webapp/WEB-INF/spring/oauth-clients.xml. The above command takes care of this.

### Clients, Users & Permissions in UAA

When we ran the command `make setup-users-and-clients` we achieved the following:

- Created `rabbit_client` client -in UAA- who is going to be used by RabbitMQ server to authenticate management users coming to the management ui.
- Created `rabbit_admin` user -in UAA- who is going to be the full administrator user with full access
- Created `rabbit_monitor` user -in UAA- who is going to be the monitoring user with just the *monitoring* *user tag*
- Created `consumer` client -in UAA- who is going to be the RabbitMQ User for the consumer application
- Created `producer` client -in UAA- who is going to be the RabbitMQ User for the producer application
- Obtained tokens -from UAA- for the 2 end users and for the 2 clients



## Understand a bit more about OAuth in the context of RabbitMQ

### About Users and Clients

First of all, we need to clarify the distinction between *users* and *clients*.
- A *user* is often represented as a live person. This is typically the user who wants to access the RabbitMQ Management UI/API.  
- A *client* (a.k.a. *service account*) is an application that acts on behalf of a user or act on its own. This is typically an AMQP application.

### About Permissions

*Users* and *clients* will both need to get granted permissions. In OAuth 2.0, permissions/roles are named *scopes*. They are free form strings. When a RabbitMQ user connects to RabbitMQ, it must provide a JWT token with those *scopes* as a password (and empty username). And RabbitMQ determines from those *scopes* what permissions it has.

The *scope* format recognized by RabbitMQ is as follows
  ```
  <resource_server_id>.<permission>:<vhost_pattern>/<name_pattern>[/<routing_key_pattern>]
  ```

where:
- `<resource_server_id>` is a prefix used for *scopes* in UAA to avoid scope collisions (or unintended overlap)
- `<permission>` is an access permission (configure, read, write, tag)
- `<vhost_pattern>` is a wildcard pattern for vhosts token has access to
- `<name_pattern>` is a wildcard pattern for resource name
- `<routing_key_pattern>` is an optional wildcard pattern for routing key in topic authorization

For more information, check the [plugin ](https://github.com/rabbitmq/rabbitmq-auth-backend-oauth2#scope-to-permission-translation) and [rabbitmq permissions](https://www.rabbitmq.com/access-control.html#permissions) docs.

Sample *scope*(s):
- `rabbitmq.read:*/*` grants `read` permission on any *vhost* and on any *resource*
- `rabbitmq.write:uaa_vhost/x-*` grants `write` permissions on `uaa_vhost` on any *resource* that starts with `x-`
- `rabbitmq.tag:monitoring` grants `monitoring` *user tag*

> Be aware that we have used `rabbitmq` resource_server_id in the sample scopes. RabbitMQ must be configured with this same `resource_server_id`. Check out [conf/symmetric_keys/rabbitmq.config](rabbitmq.config)


### About signing key required to configure RabbitMQ

This section is only to explain one of things we need to take care to configure RabbitMQ with OAuth2 auth-backend. Do not run any of the commands explained on this section. They are all included in the `make` commands we will cover in the following sections.

To configure Oauth plugin in RabbitMQ we need to obtain the JWT signing key used by UAA when it issues JWT tokens.
But our `admin` client does not have yet the right *authority* (`uaa.resource`) to get that signing key. We are going to "auto" grant it ourselves:
```
uaac client update admin --authorities "clients.read clients.secret clients.write uaa.admin clients.admin scim.write scim.read uaa.resource"
```

And now we retrieve the signing key:
```
uaac signing key -c admin -s adminsecret
```
It prints out:
```
kty: MAC
alg: HS256
value: tokenKey
use: sig
kid: legacy-token-key
```
> We could retrieve it via the UAA REST API as follows:
> `curl 'http://localhost:8080/uaa/token_key' -i  -H 'Accept: application/json' -u admin:adminsecret`


### About rotating UAA signing key

When UAA rotates the signing key we need to reconfigure RabbitMQ with that key. We don't need to edit the configuration and restart RabbitMQ.

Instead, thru the `rabbitmqctl add_uaa_key` command we can add more keys. This is more or less what could happen.

1. UAA starts up with a signing key called "key-1"
2. We configure RabbitMQ with the signing key "key-1" following the procedure explained in the previous section
3. RabbitMQ starts
4. An application obtains a token from UAA signed with that "key-1" signing key and connects to RabbitMQ using the token
5. RabbitMQ can validate it because it has the signing key
6. UAA rotates the signing key. It has a new key "key-2"
7. An application obtains a new token from UAA. This time it is signed using "key-2". The application connect to RabbitMQ using the new token
8. RabbitMQ fails to validate it because it does not have "key-2" signing key. [Later]() on we will see how RabbitMQ finds out the signing key name for the JWT
9. We add the new signing key via the `rabbitmqctl` command
10. This time RabbitMQ can validate tokens signed with "key-2"

One way to keep RabbitMQ up-to-date is to periodically check with [token keys endpoint](https://docs.cloudfoundry.org/api/uaa/version/4.28.0/index.html#token-keys) (using the `E-tag` header). When the list of active tokens key has changed, we retrieve them and add them using `rabbitmqctl add_uaa_key`.

> We are probably missing the ability to remove deprecated/obsolete signing keys. The [function](https://github.com/rabbitmq/rabbitmq-auth-backend-oauth2/blob/master/src/uaa_jwt.erl) is there so we could potentially invoke it via `rabbitmqctl eval` command.



### Understanding how an AMQP application access RabbitMQ using Oauth2

This is what it happens the under hood:
1. First of all, both applications must have their OAuth client declared in UAA. We already created them (`consumer` & `producer`) when we ran `make setup-users-and-clients` command.
2. In order to open an AMQP connection with RabbitMQ, the client must present a JWT token as the *password*. The username is ignored.
3. To obtain the JWT Token, the application requests it from UAA using its credentials (*client_id* & *client_secret*). For instance, the consumer app gets its token using this command:
  ```
  uaac token client get consumer -s consumer_secret
  ```
4. Once we have the token we can build the AMQP URI. This snipped, extracted from the [run-perftest](run-perftest) script invoked by the `start-consumer` or `start-producer` Make targets, shows how it is done:
  ```
  token=$(uaac context $CLIENT_ID | awk '/access_token/ { print $2}')
  url="amqp://ignore:$token@rabbitmq:5672/%2F"
  ```

### AMQP access via Spring and Spring Cloud Services using OAuth Client Credentials grant type

**TL;DR:**
  It is worth clarifying that this is a **service to service** interaction in the sense that the application is not using RabbitMQ on behalf a user. In other words, the application authenticates with RabbitMQ with its own identity not with the user's identity. In a classical Oauth application, the application uses the user's identity to access downstream resources. But this is not our case.

> We are demonstrating an application running in Cloud Foundry and this is the reason for referring to VCAP_SERVICES as the means to retrieve the RabbitMQ's credentials.

With that in mind, an application needs an Oauth client so that it obtains an JWT Token using Oauth Client Credentials grant type. How we tell the application which Oauth client to use is what we need to agree upon. There are two options -once again when we run RabbitMQ and our apps in Cloud Foundry :

  - **Option 1** - The **RabbitMQ service instance** provides both, the AMQP url (and management urls) and the OAuth client credentials. See below:
      ```
      {
        "user-provided": [
          {
            "credentials":  {
              "uri": "amqp://localhost:5672/%2F",
              "oauth_client": {
                "client_id": "consumer",
                "client_secret": "consumer_secret",
                "auth_domain": "http://uaa:8080/uaa"
              }
            },
            "instance_name": "rmq",
            "label": "rabbitmq-oauth",
            "name": "rmq"
          }
        ]
      }

      ```
      > rabbitmq-oauth label is a custom one created for this demonstration. The demo application extends the Spring Cloud Connector with a new AmqpOauthServiceInfo which is able to parse the `oauth_client` entry.

      THIS IS THE OPTION DEMONSTRATED WHEN WE RUN `make start-spring-demo-oauth-cf`

  - **Option 2** - **The application provides its own OAuth client**. For instance, the application could use the [Single-Sign-One service for PCF](https://docs.pivotal.io/p-identity/1-8/index.html) to assign an Oauth client to the application. In the sample
    ```
    {
      "user-provided": [
        {
          "credentials":  {
            "uri": "amqp://localhost:5672/%2F",
            "auth_enabled" : true          
          },
          "instance_name": "rmq",
          "label": "rabbitmq-oauth",
          "name": "rmq"
        }
      ],
      "sso": [
      {
        "credentials":  {
          "client_id": "myapp",
          "client_secret": "myapp_secret",
          "auth_domain": "http://uaa:8080/uaa"
        },
        "instance_name": "sso",
        "label": "sso",
        "name": "sso"
      }
      ]
    }

    ```

#### Option 1 - Oauth client provided by RabbitMQ service instance

[demo-oauth-rabbitmq](demo-oauth-rabbitmq) is a Spring Boot application that uses Spring OAuth2 support to obtain a JWT token using OAuth2 Client Credentials grant type. It leverages Spring Cloud Connectors, in particular for Cloud Foundry, to retrieve the RabbitMQ Credentials (i.e. url, oauth client credentials).

The application extends the [AmqpServiceInfo](demo-oauth-rabbitmq/src/main/java/com/pivotal/cloud/service/messaging/AmqpOAuthServiceInfo.java) so that it can get Oauth client credentials from the service instance.

The demo application consumes messages from the `q-perf-test` queue. It uses the `consumer` auth client to obtain the JWT Token.

```
make start-spring-demo-oauth-cf
```


### Understanding Access tokens and how RabbitMQ uses it

First of all, lets quickly go thru how RabbitMQ uses the OAuth Access Tokens; how RabbitMQ users/clients pass the token; whats inside the token and what information in the token is relevant for RabbitMQ and how it uses it.

**How RabbitMQ gets the token**

RabbitMQ expects a [JWS](https://tools.ietf.org/html/rfc7515) in the `password` field.

For end users, the best way to come to the management ui is by the following url, replacing `<token>` by the actual JWT. This is how `make open` command is able to open the browser and login the user using a JWT.
```
http://localhost:15672/#/login//<token>
```

**RabbitMQ expects a signed token**

RabbitMQ expects a JWS, i.e. signed JWT. It consists of 3 parts: a header which describes the signing algorithm and the signing key identifier used to sign the JWT. A body with the actual token and a signature.

This is a example of the header of a JWT issued by UAA:
  > By the way, the command `uaac token decode` does not print the header only the actual token.
  > One simple way to get this information is going to this url https://jwt.io/.

```json
{
  "alg": "HS256",
  "jku": "https://localhost:8080/uaa/token_keys",
  "kid": "legacy-token-key",
  "typ": "JWT"
}
```

where:
  - [typ](https://tools.ietf.org/html/rfc7515#page-8) is the media type which in this case is JWT. However the JWT protected header and JWT payload are secured using HMAC SHA-256 algorithm
  - [alg](https://tools.ietf.org/html/rfc7515#page-10) is the signature algorithm
  - [jku](https://tools.ietf.org/html/rfc7515#page-10) is the HTTP GET resource that returns the signing keys supported by the server that issued this token
  - [kid](https://tools.ietf.org/html/rfc7515#page-11) identifies the signing key used to sign this token


To get the signing key used by UAA we access the *token key* access point with the credentials of the `admin` UAA client; or a client which has the permission to get it.
```bash
curl http://localhost:8080/uaa/token_key \
 -H 'Accept: application/json' \
 -u admin:adminsecret  | jq .
```

It should print out:
```json
{
  "kty": "MAC",
  "alg": "HS256",
  "value": "tokenKey",
  "use": "sig",
  "kid": "legacy-token-key"
}
```

We can see that the `kid`s value above matches the `kid`'s in the JWT.

**Relevant token information for RabbitMQ**

Let's examine the following token which corresponds to end-user `rabbit_admin`.
```
{
  "jti": "dfb5f6a0d8d54be1b960e5ffc996f7aa",
  "sub": "71bde130-7738-47b8-8c7d-ad98fbebce4a",
  "scope": [
    "rabbitmq.read:*/*",
    "rabbitmq.write:*/*",
    "rabbitmq.tag:administrator",
    "rabbitmq.configure:*/*"
  ],
  "client_id": "rabbit_client",
  "cid": "rabbit_client",
  "azp": "rabbit_client",
  "grant_type": "password",
  "user_id": "71bde130-7738-47b8-8c7d-ad98fbebce4a",
  "origin": "uaa",
  "user_name": "rabbit_admin",
  "email": "rabbit_admin@example.com",
  "auth_time": 1551957721,
  "rev_sig": "d5cf8503",
  "iat": 1551957721,
  "exp": 1552000921,
  "iss": "http://localhost:8080/uaa/oauth/token",
  "zid": "uaa",
  "aud": [
    "rabbitmq",
    "rabbit_client"
  ]
}
```

These are the fields relevant for RabbitMQ:
- `sub` ([Subject](https://tools.ietf.org/html/rfc7519#page-9)) this is the identify of the subject of the token. **RabbitMQ uses this field to identify the user**. This token corresponds to the `rabbit_admin` end user. If we logged into the management ui, we would see it in the top-right corner. If this were an AMPQ user, we would see it on each connection listed in the connections tab.  
  UAA would add 2 more fields relative to the *subject*: a `user_id` with the same value as the `sub` field, and `user_name` with user's name. In UAA, the `sub`/`user_id` fields contains the user identifier, which is a GUID.

- `client_id` (not part of the RFC-7662) identifies the OAuth client that obtained the JWT. We used `rabbit_client` client to obtain the JWT for `rabbit_admin` user. **RabbitMQ also [uses](https://github.com/rabbitmq/rabbitmq-auth-backend-oauth2/blob/master/src/rabbit_auth_backend_oauth2.erl#L169) this field to identify the user**.

- `aud` ([Audience](https://tools.ietf.org/html/rfc7519#page-9)) this identifies the recipients and/or resource_server of the JWT. **RabbitMQ uses this field to validate the token**. When we configured RabbitMQ OAuth plugin, we set `resource_server_id` attribute with the value `rabbitmq`. The list of audience must have the `rabbitmq` otherwise RabbitMQ rejects the token.

- `jti` ([JWT ID](https://tools.ietf.org/html/rfc7662#section-2.2)) this is just an identifier for the JWT

- `iss` ([Issuer](https://tools.ietf.org/html/rfc7662#section-2.2)) identifies who issued the JWT. UAA will set it to end-point that returned the token.

- `scope` is an array of [OAuth Scope](https://tools.ietf.org/html/rfc7523#page-4). **This is what RabbitMQ uses to determine the user's permissions**. However, RabbitMQ will only use the *scopes* which belong to this RabbitMQ identified by the plugin configuration parameter `resource_server_id`. In other words, if the `resource_server_id` is `rabbitmq`, RabbitMQ will only use the *scopes* which start with `rabbimq.`.

- `exp` ([exp](https://tools.ietf.org/html/rfc7519#page-9)) identifies the expiration time on
   or after which the JWT MUST NOT be accepted for processing. RabbitMQ uses this field to validate the token if it is present.
   > Implementers MAY provide for some small leeway, usually no more than
   a few minutes, to account for clock skew. However, RabbitMQ does not add any leeway.


### Useful uaac commands

 `uaac` allows us to generate or obtain many tokens for different users and/or clients. However, only one of them is treated as the **current** token. This **current** token is only relevant when we interact with `uaac`, say to create/delete users, and/or obtain further tokens.

 To know all the tokens we have generated so far we run:
 ```
 uaac contexts
 ```

 To know what the current context is, we run:
 ```
 uaac context
 ```
 It prints out :
 ```
 0]*[http://localhost:8080/uaa]

   [0]*[admin]
       client_id: admin
       access_token: eyJhbGciOiJIUzI1NiIsImprdSI6Imh0dHBzOi8vbG9jYWxob3N0OjgwODAvdWFhL3Rva2VuX2tleXMiLCJraWQiOiJsZWdhY3ktdG9rZW4ta2V5IiwidHlwIjoiSldUIn0.eyJqdGkiOiIxODkyY2ZmMmRmNjc0ZmRiYmYwMWIyM2I2ZWU4MjlkZCIsInN1YiI6ImFkbWluIiwiYXV0aG9yaXRpZXMiOlsiY2xpZW50cy5yZWFkIiwiY2xpZW50cy5zZWNyZXQiLCJjbGllbnRzLndyaXRlIiwidWFhLmFkbWluIiwiY2xpZW50cy5hZG1pbiIsInNjaW0ud3JpdGUiLCJzY2ltLnJlYWQiXSwic2NvcGUiOlsiY2xpZW50cy5yZWFkIiwiY2xpZW50cy5zZWNyZXQiLCJjbGllbnRzLndyaXRlIiwidWFhLmFkbWluIiwiY2xpZW50cy5hZG1pbiIsInNjaW0ud3JpdGUiLCJzY2ltLnJlYWQiXSwiY2xpZW50X2lkIjoiYWRtaW4iLCJjaWQiOiJhZG1pbiIsImF6cCI6ImFkbWluIiwiZ3JhbnRfdHlwZSI6ImNsaWVudF9jcmVkZW50aWFscyIsInJldl9zaWciOiI4Yzg2YjcyOCIsImlhdCI6MTU1MDc1OTI0OCwiZXhwIjoxNTUwODAyNDQ4LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvdWFhL29hdXRoL3Rva2VuIiwiemlkIjoidWFhIiwiYXVkIjpbInNjaW0iLCJjbGllbnRzIiwidWFhIiwiYWRtaW4iXX0._d9UPkdDNTYsCjf1NemWIBfv0v8S4u0wzjrBmP4S11U
       token_type: bearer
       expires_in: 43199
       scope: clients.read clients.secret clients.write uaa.admin clients.admin scim.write scim.read
       jti: 1892cff2df674fdbbf01b23b6ee829dd

 ```
 We can decode the jwt token above:
 ```
 uaac token decode eyJhbGciOiJIUzI1NiIsImprdSI6Imh0dHBzOi8vbG9jYWxob3N0OjgwODAvdWFhL3Rva2VuX2tleXMiLCJraWQiOiJsZWdhY3ktdG9rZW4ta2V5IiwidHlwIjoiSldUIn0.eyJqdGkiOiIxODkyY2ZmMmRmNjc0ZmRiYmYwMWIyM2I2ZWU4MjlkZCIsInN1YiI6ImFkbWluIiwiYXV0aG9yaXRpZXMiOlsiY2xpZW50cy5yZWFkIiwiY2xpZW50cy5zZWNyZXQiLCJjbGllbnRzLndyaXRlIiwidWFhLmFkbWluIiwiY2xpZW50cy5hZG1pbiIsInNjaW0ud3JpdGUiLCJzY2ltLnJlYWQiXSwic2NvcGUiOlsiY2xpZW50cy5yZWFkIiwiY2xpZW50cy5zZWNyZXQiLCJjbGllbnRzLndyaXRlIiwidWFhLmFkbWluIiwiY2xpZW50cy5hZG1pbiIsInNjaW0ud3JpdGUiLCJzY2ltLnJlYWQiXSwiY2xpZW50X2lkIjoiYWRtaW4iLCJjaWQiOiJhZG1pbiIsImF6cCI6ImFkbWluIiwiZ3JhbnRfdHlwZSI6ImNsaWVudF9jcmVkZW50aWFscyIsInJldl9zaWciOiI4Yzg2YjcyOCIsImlhdCI6MTU1MDc1OTI0OCwiZXhwIjoxNTUwODAyNDQ4LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvdWFhL29hdXRoL3Rva2VuIiwiemlkIjoidWFhIiwiYXVkIjpbInNjaW0iLCJjbGllbnRzIiwidWFhIiwiYWRtaW4iXX0._d9UPkdDNTYsCjf1NemWIBfv0v8S4u0wzjrBmP4S11U
 ```
 It prints out:
 ```
 jti: 1892cff2df674fdbbf01b23b6ee829dd
 sub: admin
 authorities: clients.read clients.secret clients.write uaa.admin clients.admin scim.write scim.read
 scope: clients.read clients.secret clients.write uaa.admin clients.admin scim.write scim.read
 client_id: admin
 cid: admin
 azp: admin
 grant_type: client_credentials
 rev_sig: 8c86b728
 iat: 1550759248
 exp: 1550802448
 iss: http://localhost:8080/uaa/oauth/token
 zid: uaa
 aud: scim clients uaa admin
 ```

## Findings

### Management UI shows token's client_id as user in the top right corner

Given that we have a token for `rabbit_admin`. When we use it to login to RabbitMQ we should see `User rabbit_admin` in the top right corner of the Management UI. However, it shows `User rabbit_client` which is the identity used to get a token for the user. This is due to how RabbitMQ determines the username. It first looks up `client_id` and it does not exist, it looks up `sub`. I would say `sub` should be the first field to check, and `client_id` as last. It works for both, end-users coming over http and applications coming over AMQP.

Although, tokens issued by UAA sets the *user id* (GUID) in the `sub` field rather than the actual *user name* (the *user name* is in the `user_name` field of the JWT).

### Logout from management ui should redirect the user back to the initial login

Given that we have successfully logged in, when we click on the logout link, it does not take the user out to the initial page shown to non-authorized users. Instead the user stays in the same page, for instance the overview page, it was. The user is actually logged out from a session/security standpoint because if we refresh ui, we are no longer authenticated.
