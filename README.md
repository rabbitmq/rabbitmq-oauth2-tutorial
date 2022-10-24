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
-	[Access Management UI using OAuth 2.0 tokens](#access-management-ui-using-oauth-20-tokens)
	- [Service-Provider initiated logon](#service-provider-initiated-logon)
 	- [Identity-Provider initiated logon](#identity-provider-initiated-logon)
- [Access other protocols using OAuth 2.0 tokens](#access-other-protocols)
	- [Monitoring agent accessing management REST api](#monitoring-agent-accessing-management-rest-api)
	- [AMQP protocol](#amqp-protocol)
	- [JMS protocol](#jms-protocol)
	- [MQTT protocol](#mqtt-protocol)
- Use advanced OAuth 2.0 configuration
	- [Use custom scope field](#use-custom-scope-field)
	- [Use multiple asymmetrical signing keys](#use-multiple-asymmetrical-signing-keys)
	- [Use custom scopes](#use-custom-scopes)
	- [Use Rich Authorization Request Tokens](#use-rich-authorization-request-tokens)
- Use different OAuth 2.0 servers
	- [KeyCloak](use-cases/keycloak.md)
	- [https://auth0.com/](use-cases/auth0.md)
	- [Azure Active Directory](use-cases/azure.md)

- [Understand the environment](#understand-the-environment)
	- [RabbitMQ server](#rabbitmq-server)
	- [UAA server](#uaa-server)
	- [UAA client](#uaa-client)
	- [Clients, Users & Permissions in UAA](#clients-users-permissions-in-uaa)
- [Understand a bit more about OAuth in the context of RabbitMQ](#understand-a-bit-more-about-oauth-in-the-context-of-rabbitmq)
	- [About Users and Clients](#about-users-and-clients)
	- [About Permissions](#about-permissions)
	- [About signing key required to configure RabbitMQ](#about-signing-key-required-to-configure-rabbitmq)
	- [About rotating JWT signing key](#about-rotating-uaa-signing-key)
	- [Understanding Access tokens and how RabbitMQ uses it](#understanding-access-tokens-and-how-rabbitmq-uses-it)

<!-- /TOC -->

## Prerequisites to follow this guide

- Docker must be installed
- Ruby must be installed
- make


## OAuth2 plugin in action

In order see the [rabbitmq-auth-backend-oauth2](https://github.com/rabbitmq/rabbitmq-server/tree/main/deps/rabbitmq_auth_backend_oauth2) plugin in action we need an OAuth 2.0 **Authorization server** running and RabbitMQ server configured accordingly. To get up and running quickly, we are going to use UAA as Authorization Server. In the next section, we will see how to set up UAA and RabbitMQ. If you are new to OAuth 2.0, it is a good starting point. If you already know OAuth 2.0 and you want to learn how to configure RabbitMQ to talk to one of OAuth 2.0 server tested on this tutorial, you can jump straight to them. They are [KeyCloak](use-cases/keycloak.md), [https://auth0.com/](use-cases/auth0.md) and [Azure Active Directory](use-cases/azure.md) in addition to UAA which we will use it in the next sections.


### Set up UAA and RabbitMQ

RabbitMQ support two types of two signing keys used to digitally sign the JWT tokens.
The two types are **symmetrical** and **asymmetrical** signing keys. The Authorization server is who digitally signs the JWT tokens and RabbitMQ has to be configured to validate any of the two types of digital signatures.

Given that asymmetrical keys are the most widely used option, we are going to focus on how to
configure RabbitMQ with them.

#### Use Asymmetrical digital signing keys

Run the following 2 commands to get the environment ready to see Oauth2 plugin in action:

  1. `make start-uaa` to get UAA server running
  2. `make start-rabbitmq` to start RabbitMQ server


## Access Management UI using OAuth 2.0 tokens

The Management UI can be configured with one of these two login modes:

* [Service-Provider initiated logon](#service-provider-initiated-logon) - This is the default and traditional OAuth 2.0 logon mode. The user comes to the Management UI and clicks on the button "Click here to logon" which initiates the logon. The logon process starts in RabbitMQ, the Service Provider.
* [Identity-Provider initiated logon](#identity-provider-initiated-logon) - This is a logon mode meant for web portals. Users navigate to RabbitMQ with a token already obtained by the web portal on behalf of the user.

### Supported OAuth 2.0 flow

Since RabbitMQ 3.10 the Management UI uses *Authorization Code flow with PKCE**. Because RabbitMQ is a single-page
web application, it cannot safely store credentials such as the `client_id` and `client_secret` required by
RabbitMQ to authenticate with the Authorization Server in order to get a token for the end-user. Therefore, we
should configure the RabbitMQ OAuth client in the Authorization Server so that it does not require `client_secret`.
This type of OAuth clients/applications are known as **public** or **non-confidential**. In UAA they are configured as `allowpublic: true`.

Nevertheless, should your Authorization Server require a `client_secret` , we can configure it via `management.oauth_client_secret`.

### Service-Provider initiated logon

The first time an end user arrives to the management ui (`1`), The user clicks on the button `Click here to login` and it is redirected (`2`) to UAA to authenticate. Once they successfully authenticate with UAA, the user is redirected back (`3.`) to RabbitMQ with a valid JWT token. RabbitMQ validates it and identifies the user and extracts its permissions from the JWT token.

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
 - `rabbit_admin:rabbit_admin` with full administrator access, i.e. `administrator` user-tag
 - and `rabbitmq_management:rabbitmq_management` with just `management` user-tag

Go to http://localhost:15672 and login using any of those two users. To try with a different user, just click on "logout" button and click again on `Click here to log in` and login with the other user.

This is a token issued by UAA for the `rabbit_admin` user thru the redirect flow we just saw above.
It was signed with the symmetric key.

![JWT token](assets/admin-token-signed-sym-key.png)

To configure RabbitMQ Management UI with OAuth 2.0 we need the following configuration entries:
```
 ...
 {rabbitmq_management, [
    {oauth_enabled, true},
    {oauth_client_id, "rabbit_client_code"},
    {oauth_provider_url, "http://localhost:8080"},      
    ...
  ]},
```

### Identity-Provider initiated logon

**Note**: **This feature has not been released yet. It is only available in the development docker image
pivotalrabbitmq/rabbitmq:oidc_idp_initiated_login-otp-max-bazel **

When RabbitMQ is offered as a service from a web portal, it is more convenient to navigate to RabbitMQ Management UI with a single click. The web portal is responsible for getting a token before taking the user to the RabbitMQ Management UI web page.

```
    [ Idp | WebPortal ] ----> 2. /#/login?access_token=<TOKEN>----   [ RabbitMQ Cluster ]            
              /|\                                                        |       /|\
               |                                                         +--------+
      1. rabbit_admin from a browser                                   3. validate token        
```

At step 1, `rabbit_admin` user navigates to the web portal and clicks on the hyperlink associated to a RabbitMQ cluster. At step2, the web portal obtains a token and redirects the user to RabbitMQ. And at step 3, RabbitMQ validates the token in the http request and if it is valid, it redirects the user to the overview page.

By default, RabbitMQ Management UI is configured with service-provider initiated logon. We have to configure the Management plugin by adding just one entry to the configuration as shown below:

```
 ...
 {rabbitmq_management, [
    {oauth_enabled, true},
    {oauth_client_id, "rabbit_client_code"},
    {oauth_provider_url, "http://localhost:8080"},      
    {oauth_initiated_logon_type, idp_initiated},
    ...
  ]},
```

**NOTE**: When the user logs out, or its RabbitMQ session expired, or the token expired, the user is directed to the Management landing page which presents the user with a button labeled Click here to login. The user is never redirected automatically back to the url configured in `oauth_provider_url`. Only when the user clicks on the button, it is redirected to the configured in `oauth_provider_url`.

## Access other protocols using OAuth 2.0 tokens

The following subsections demonstrate how to use access tokens with any messaging protocol and also to access the management rest api.


### Monitoring agent accessing management REST api

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
make curl-uaa url=http://localhost:15672/api/overview client_id=mgt_api_client secret=mgt_api_client
```


### AMQP Protocol

**DL;DR:**
In this section, we are demonstrating how an application can connect to RabbitMQ presenting a JWT Token as a credential. The application we are going to use is [PerfTest](https://github.com/rabbitmq/rabbitmq-perf-test) which is not an OAuth 2.0 aware application -see [next use case](#) for an OAuth 2.0 aware application.

We are launching PerfTest with a token that we have previously obtained from UAA. This is just to probe AMQP access with a JWT Token. Needless to say that the application should instead obtain the JWT Token prior to connecting to RabbitMQ and it should also be able to refresh it before reconnecting. RabbitMQ validates the token before accepting it. If the token has expired, RabbitMQ will reject the connection.

First of all, an application which wants to connect to RabbitMQ using OAuth2 must present a
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


### JMS protocol

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

### MQTT protocol

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
previous use cases, you need to launch rabbitmq first like this `make start-rabbitmq`.
```
make start-mqtt-publish TOKEN=$(bin/jwt_token scopes-for-mqtt.json legacy-token-key private.pem public.pem)
```

> IMPORTANT: If you try to access the management ui and authenticate with UAA using rabbit_admin you
wont be able to do bind a queue with routing_key `test` to the `amq.topic` exchange because that user
in UAA does not have the required permissions. In our handcrafted token, we have granted ourselves the right permissions/scopes.

## Use advanced OAuth 2.0 configuration

In this section, we are going to explore various OAuth 2.0 configurations we can enable in RabbitMQ.

### Use custom scope field  

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

### Use multiple asymmetrical signing keys

This scenario explores the use case where JWT tokens may be signed by different asymmetrical signing keys.

There are 2 ways to configure RabbitMQ with multiple signing keys:
- We can either **statically** configure them via `rabbitmq.conf` as shown in the [plugin documentation page](https://github.com/rabbitmq/rabbitmq-server/tree/master/deps/rabbitmq_auth_backend_oauth2#variables-configurable-in-rabbitmqconf).
- Or we can do it **dynamically**, i.e, add signing keys while RabbitMQ is running and without having to
restart it. This alternative is explained in more detail in the section [About rotating JWT signing key](#about-rotating-jwt-signing-key).
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



### Use custom scopes

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
- [conf/uaa/rabbitmq-scope-aliases.config](conf/uaa/rabbitmq-scope-aliases.config) - which configures scope mappings.
- [conf/uaa/rabbitmq-scope-aliases-and-extra-scope.config](conf/uaa/rabbitmq-scope-aliases-and-extra-scope.config) - which configures `extra_scopes_source` and scope mappings.


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


### Use Rich Authorization Request Tokens

The [Rich Authorization Request](https://oauth.net/2/rich-authorization-requests/) extension provides a way for OAuth clients to request fine-grained permissions during an authorization request. It moves away from the concept of Scopes and instead
define a rich permission model.

RabbitMQ supports JWT tokens compliant with this specification. Here is a sample JWT token where we have stripped out
all the other attributes and left only the relevant ones for this specification:

```
{
  "authorization_details": [
    { "type" : "rabbitmq",  
      "locations": ["cluster:finance/vhost:primary-*"],
      "actions": [ "read", "write", "configure"  ]
    },
    { "type" : "rabbitmq",
      "locations": ["cluster:finance", "cluster:inventory" ],
      "actions": ["administrator" ]
    }
  ]
}
```

*Get the environment ready*

To demonstrate this new capability we have to deploy RabbitMQ with the appropriate configuration file
under [conf/uaa/rabbitmq-for-rar-tokens.config](conf/uaa/rabbitmq-for-rar-tokens.config).

```
export CONFIG=rabbitmq-for-rar-tokens.config
make start-rabbitmq
```

**NOTE**: We do not need to run any OAuth2 server like UAA. This is because we are creating a token and signing it using the same
private-public key pair RabbitMQ is configured with.

*Use a Rich Authorization Token to access the management rest api*

We are going use this token [jwts/rar-token.json](jwts/rar-token.json) to access an endpoint of the management rest api.

```
make curl-with-token URL=http://localhost:15672/api/overview TOKEN=$(bin/jwt_token rar-token.json legacy-token-key private.pem public.pem)
```
> We are using curl to go to the URL using a TOKEN which we have built using the command bin/jwt_token which takes the JWT payload, the name of the signing key and the private and public certificates to sign the token

*Use a Rich Authorization Token to access AMQP protocol*

This time, we are going to use the same token we used in the previous section to access the AMQP protocol via the PerfTest tool which acts as a AMQP producer application:
```
make start-perftest-producer-with-token PRODUCER=producer_with_roles TOKEN=$(bin/jwt_token rar-token.json legacy-token-key private.pem public.pem)
```

The command above launches the application in the background, we can check the logs by running this command:
```
docker logs producer_with_roles -f
```

For more information on this new capability check out the [plugin's documentation](https://github.com/rabbitmq/rabbitmq-server/tree/rich_auth_request/deps/rabbitmq_auth_backend_oauth2#rich-authorization-request).


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
      {oauth_enabled, true},
      {oauth_client_id, "rabbit_client_code"},
      {oauth_provider_url, "http://uaa:8080/uaa"}
  ]},
].
```

### UAA server

Standalone OAuth2 server (https://github.com/cloudfoundry/uaa). Its primary role is to serve as an OAuth2 provider, issuing tokens for client applications. It can also authenticate users with their Cloud Foundry credentials. It has endpoints for managing user accounts and for registering OAuth2 clients, as well as various other management functions

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

When we run `make start-uaa` we start up UAA with a [uaa.yaml](conf/uaa/uaa.yml) configured with all the clients, users
and permissions required by this tutorial. For instance:

- `rabbit_client`: client who is going to be used by RabbitMQ server to authenticate management users coming to the management ui.
- `rabbit_admin`: user who is going to be the full administrator user with full access
- `rabbit_monitor`: user who is going to be the monitoring user with just the *monitoring* *user tag*
- `consumer`: client who is going to be the RabbitMQ User for the consumer application
- `producer`: client who is going to be the RabbitMQ User for the producer application

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

This is the signing key UAA uses to sign the tokens. RabbitMQ is already configured with this key.

```
uaac target  http://localhost:8080
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


### About rotating JWT signing key

When UAA -or any other OAuth2 server- rotates the signing key we need to reconfigure RabbitMQ with that key. We don't need to edit the configuration and restart RabbitMQ.

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


### Understanding Access tokens and how RabbitMQ uses it

First of all, lets quickly go thru how RabbitMQ uses the OAuth Access Tokens; how RabbitMQ users/clients pass the token; whats inside the token and what information in the token is relevant for RabbitMQ and how it uses it.

**How RabbitMQ gets the token**

RabbitMQ expects a [JWS](https://tools.ietf.org/html/rfc7515) in the `password` field.

For end users, the best way to come to the management ui is by the following url, replacing `<token>` by the actual JWT.
```
http://localhost:15672/#/login/?&access_token=<token>
```

This is how `make open` command is able to open the browser and login the user using a JWT.
```
make open username=rabbit_admin password=rabbit_admin
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
