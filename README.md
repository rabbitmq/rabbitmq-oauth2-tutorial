# RabbitMQ integration with OAuth 2.0

The main goal of this guide is to explore how to set up RabbitMQ to authenticate and authorize via OAuth 2.0 protocol using [UAA](https://github.com/cloudfoundry/uaa) as an OAuth Authentication Server.

## Getting started with the environment

### Deploy UAA server
To deploy UAA with docker follow these instructions:
```
wget https://github.com/cloudfoundry/uaa/archive/4.24.0.tar.gz
tar xvfz 4.24.0.tar.gz
rm 4.24.0.tar.gz
cd uaa-4.24.0
docker run -it -v $PWD:/uaa -p 8080:8080 openjdk:8-jdk /uaa/gradlew run
```

**IMPORTANT**:
- It will be improved the way we launch UAA with docker
- UAA can run with an external database. But for the purposes of this exploration, the internal database is sufficient

Wait until you see the following in the standard output:
```
> :cargoRunLocal
```

To check that UAA is running fine:
```
curl -k  -H 'Accept: application/json' http://localhost:8080/uaa/info | jq .
```

### Install and Setup UAA client to interact with UAA server
This is a client to interact with UAA via command line:
```
sudo gem install cf-uacc
```

We need to point `uaac` to our uaa server:
```
uaac target  http://localhost:8080/uaa
```

In order to operate with uaa we need to "login". There is a OAuth client preconfigured with the following credentials `admin:adminsecret`. This user is configured under <uaa_repo>/uaa/src/main/webapp/WEB-INF/spring/oauth-clients.xml.
```
uaac token client get admin -s adminsecret
```

Now we can obtain the current context configured in the uacc client:
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
To configure Oauth plugin in RabbitMQ we need to obtain the JWT signing key used by UAA when it issues JWT tokens.
But our `admin` client does not have yet the right *authority* to get that signing key. We are going to "auto" grant it ourselves:
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

**Users and Clients**

First of all, we need to clarify the distinction between *users* and *clients*.
- A *user* is often represented as a live person, or a process running. This is typically the user that wants to access the RabbitMQ Management UI/API.  
- A *client* (a.k.a. *service account*) is an application that acts on behalf of a user or act on its own. This is typically the AMQP application.

**Permissions**

Users and clients will both need to get granted permissions. In OAuth 2.0, permissions/roles are named *scopes*. They are free form strings. When a RabbitMQ user connects to RabbitMQ, it must provide a JWT token with those *scopes* as a password. And RabbitMQ determines from those *scopes* what permissions it has.

The *scope* format recognized by RabbitMQ is
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

Sample *scope* examples:
- `rabbitmq.read:*/*` grants `read` permission on any *vhost* and on any *resource*
- `rabbitmq.write:uaa_vhost/*` grants `write` permissions on `uaa_vhost` on any *resource*
- `rabbitmq.tag:monitoring/*` grants `monitoring` *user tag* on any *vhost*

> Be aware that we have used `rabbitmq` resource_server_id in the sample scopes. RabbitMq must be configured with this same `resource_server_id`.


#### Create Scopes in UAA for RabbitMQ Permissions
In UAA, *group* are synonym of *OAuth scope*. We need to create as many *UAA groups* as *Oauth scopes* we need.

```
uaac group add "rabbitmq.read:*/*"
uaac group add "rabbitmq.write:*/*"
uaac group add "rabbitmq.configure:*/*"
uaac group add "rabbitmq.tag:management"
uaac group add "rabbitmq.tag:monitoring"
uaac group add "rabbitmq.tag:administrator"

uaac group add "rabbitmq.write:%2F/*"

```

#### Create Users and assign them groups

`rabbitmq_admin` is going to be the full administrator access user. This user gets created when we run `make start-uaa` command.

These commands are under [setup-uaa](setup-uaa) script.

```
uaac user add rabbit_admin -p rabbit_admin --email rabbit_admin@example.com
uaac member add "rabbitmq.read:*/*" rabbit_admin
uaac member add "rabbitmq.write:*/*" rabbit_admin
uaac member add "rabbitmq.configure:*/*" rabbit_admin
uaac member add "rabbitmq.tag:administrator" rabbit_admin
```

`rabbit_monitor` is going to be the monitoring access user.

```
uaac user add rabbit_monitor -p rabbit_monitor --email rabbit_monitor@example.com
uaac member add "rabbitmq.tag:monitoring" rabbit_monitor
```

#### Create OAuth client for 2 sample applications

We have two applications, `producer` and `consumer`, with the following permissions:
- `producer` should be able to write/configure on any resource in the default vhost.
- `consumer` should be able to read/configure on any resource on any vhosts.
- both need the `management` *user tag*

The Oauth client for these 2 applications are already created when we run `make start-uaa`. It is implemented by [setup-uaa](setup-uaa) script.

```
echo "Adding Oauth client for producer and consumer apps"

uaac client add admin_client --name producer --scope "rabbitmq.write:%2F/*" --authorized_grant_types client_credentials --authorities rabbitmq --secret producer_secret
uaac client add admin_client --name consumer --scope "rabbitmq.read:*/*" --authorized_grant_types client_credentials --authorities rabbitmq --secret consumer_secret
```

## Findings

### Management UI shows token's client_id as user in the top right corner

Given that we have a token for `rabbit_admin`. When we use it to login to RabbitMQ we should see `User rabbit_admin` in the top right corner of the Management UI. However, it shows `User rabbit_client` which is the identity used to get a token for the user.
```
make get-token-for-rabbit-admin
open as=rabbit_admin
```
