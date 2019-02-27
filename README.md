# RabbitMQ integration with OAuth 2.0

The two goals of this integration guide:
- Explore how applications and end users can authenticate with RabbitMQ server using OAuth 2.0 protocol rather than the traditional username/password, or others.
- Explore what it takes to set up RabbitMQ Server with OAuth 2.0 authentication mechanism. This not only means setting up RabbitMQ server itself but also an OAuth 2.0 Authorization Server ([UAA](https://github.com/cloudfoundry/uaa)) and all the operations to create OAuth clients, users and obtain their tokens.

We will start first exploring what it takes to set up RabbitMQ along with the OAuth 2.0 authentication server. Once we have that running, we move onto creating users and clients. And finally, we test 2 usage scenarios:
- an end user accessing the management UI
- a consumer application and a producer application.


*Table of Context*

- [Prerequisites to use this repository](#Prerequisites-to-use-this-repository)
- [Set up environment](#set-up-environment)
  - [Deploy UAA server](#deploy-uaa-server)
  - [Install and Setup UAA client to interact with UAA server](#Install-and-Setup-UAA-client-to-interact-with-UAA-server)
  - [Deploy RabbitMQ with OAuth2 plugin and UAA signing key](#Deploy-RabbitMQ-with-OAuth2-plugin-and-UAA-signing-key)
  - [Create users/clients and grant them permissions](#Create-users%2Fclients-and-grant-them-permissions)
- [Access RabbitMQ with OAuth](#access-rabbitmq-with-oauth)


## Prerequisites to use this repository

- Docker must be installed


## Set up environment

### Deploy UAA server

The following command will install UAA and start it without any users, clients and permissions:
```
make start-uaa
```

**IMPORTANT**:
- UAA can run with an external database. But for the purposes of this exploration, the internal database is sufficient

The UAA server takes some time to start. Run the following command to check the logs of the UAA server:
```
docker logs uaa -f
```

And wait until you see the following in the standard output:
```
> :cargoRunLocal
```

To check that UAA is running fine:
```
curl -k  -H 'Accept: application/json' http://localhost:8080/uaa/info | jq .
```

### Install and Setup UAA client to interact with UAA server

We have the UAA server running. In order to interact with it there is a convenient command-line application called `uaac`. To install it and get it ready run the following command:
```
make install-uaac
```

> In order to operate with uaa we need to "authenticate". There is an OAuth client preconfigured with the following credentials `admin:adminsecret`. This user is configured under <uaa_repo>/uaa/src/main/webapp/WEB-INF/spring/oauth-clients.xml. The above command takes care of this.

#### Useful uaac commands

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

### Deploy RabbitMQ with OAuth2 plugin and UAA signing key

#### Obtain the UAA signing key required to configure RabbitMQ

This section is only to explain one of things we need to take care to configure RabbitMQ with OAuth2 auth backend. Do not run any of the commands explained on this section. They are all included in the `make` commands we will cover in the following sections.

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

This is the minimal RabbitMQ configuration we will need. we have kept the internal auth backend although it is not necessary:
```
[
  % Enable auth backend
  {rabbit, [
     {auth_backends, [rabbit_auth_backend_oauth2, rabbit_auth_backend_internal]}
  ]},
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
].
```

#### Build RabbitMQ docker image with Oauth 2.0 plugin

First, we need to build the plugin (`.ez` file) because it does not come, by default, in the list of installed plugins in RabbitMQ server. And then we build a Docker image starting from [rabbitmq:3.8-rc](https://hub.docker.com/_/rabbitmq).

```
make build-docker-image
```

#### Start RabbitMQ Server  

We are going to launch RabbitMQ using 3.8 of the [official docker image](https://hub.docker.com/_/rabbitmq) but with
- custom `rabbitmq.conf` file,
- `rabbitmq-auth-backend-oauth2` plugin enabled

```
make start-rabbitmq`
```

### Create users/clients and grant them permissions

**About Users and Clients**

First of all, we need to clarify the distinction between *users* and *clients*.
- A *user* is often represented as a live person. This is typically the user who wants to access the RabbitMQ Management UI/API.  
- A *client* (a.k.a. *service account*) is an application that acts on behalf of a user or act on its own. This is typically an AMQP application.

**About Permissions**

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

> Be aware that we have used `rabbitmq` resource_server_id in the sample scopes. RabbitMQ must be configured with this same `resource_server_id`. Check out [rabbitmq.config](rabbitmq.config)

**Create Clients, Users, Permissions and grant them**

Run the command `make setup-users-and-tokens` to achieve the following:
- Create `rabbit_admin` user who is going to be the full administrator user with full access
- Create `rabbit_monitor` user who is going to be monitoring user with just the *monitoring* *user tag*
- Create `consumer` client who is going to be the RabbitMQ User for the consumer application
- Create `producer` client who is going to be the RabbitMQ User for the producer application
- Create tokens for the 2 end users and for 2 clients

## Access RabbitMQ with OAuth

We are going to explore 2 types of access:
- End users accessing management ui
- Applications accessing AMQP protocol

### End user access to management ui

These two commands demonstrates how two distinct end users log into the Management UI using Oauth 2.0 protocol:
```
make open username=rabbit_admin password=rabbit_admin
```
```
make open username=rabbit_monitor password=rabbit_monitor
```

This is what it happens the under hood:
1. First of all, both users must be declared in UAA. We already created them (`rabbit_admin` & `rabbit_monitor`) when we ran `make setup-users-and-tokens` command.
2. In order to access the RabbitMQ management ui, the user must first login with *username* blank and with a JWT token as the *password*.
  RabbitMQ does not support a *Service Provider initiated flow* where RabbitMQ would redirect the user to some url (a.k.a. *authorization endpoint*) to authenticate if the user does not present any credentials. Instead, RabbitMQ only supports an *Identity Provider initiated flow* where the user must come with its credentials.
3. To obtain the JWT Token, the user presents its credentials (username & password) to some application which acts like a **login server** which authenticates the user with UAA. In our case, the flow is implemented by calling the following command in `uaac`. The `rabbit_client` is an Oauth client in UAA that we use to obtain a token on behalf of the end user.  
  ```
  uaac token owner get rabbit_client $USERNAME -s rabbit_secret -p $PASSWORD
  ```
4. If we successfully obtained the token, we can build the *login url* to the RabbitMQ management ui with a blank *username* and the token as the *password*
  ```
  url="http://localhost:15672/#/login//$token"
  open $url
  ```

### AMQP access

These two commands demonstrates how two distinct applications access RabbitMQ via AMQP using Oauth 2.0 protocol:

```
make start-consumer
```

```
make start-producer
```

This is what it happens the under hood:
1. First of all, both applications must have their OAuth client declared in UAA. We already created them (`consumer` & `producer`) when we ran `make setup-users-and-tokens` command.
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


## Findings

### Management UI shows token's client_id as user in the top right corner

Given that we have a token for `rabbit_admin`. When we use it to login to RabbitMQ we should see `User rabbit_admin` in the top right corner of the Management UI. However, it shows `User rabbit_client` which is the identity used to get a token for the user.
```
make get-token-for-rabbit-admin
open as=rabbit_admin
```
