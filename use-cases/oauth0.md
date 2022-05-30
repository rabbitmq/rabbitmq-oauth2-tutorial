# Use https://auth0.com/ as OAuth 2.0 server

We are going to test 3 OAuth flows:
1. Access management ui via a browser
2. Access management rest api
3. Access AMQP protocol

## Prerequisites to follow this guide

- Have an account in https://auth0.com/.
- Docker

## Set up OAuth0

### Create RabbitMQ Resource

In OAuth0, resources are mapped to Application APIs. Once you have logged onto your account in https://auth0.com/, go to dashboard > Applications > APIs > Create an API.

Give it the name `rabbitmq`. The important thing here is the `identifier` which must have the name of the *resource_server_id* we configured in RabbitMQ. This `identifier` goes into the `audience` JWT field. In our case, it is called `rabbitmq`. And we choose `RS256` as the signing algorithm.

### Configure which scopes the RabbitMQ Resource supports

Edit the API we just created with the name `rabbitmq`. Go into Permissions and add the permissions (scope) this api can grant.
We are going to add the following scopes:
- `rabbitmq.read:*/*`
- `rabbitmq.write:*/*`
- `rabbitmq.configure:*/*`
- `rabbitmq.tag:administrator`

### Create an OAuth client for the Management UI

By default, for every API we create, an *Application* gets created using the API's `identifier` as its name.
An *Application* requests an **OAuth client**.

Go to dashboard > Applications, and you should see your application listed. An application gives us a *client_id*, a *client_secret* and a http endpoint called *Domain* where to claim a token.

### Grant our OAuth client permission to use RabbitMQ resource

Go into dashboard > Applications > rabbitmq > APIs, you will see a list of all the APIs including the one we just created. Along with each API there is a toggle to authorize the Application to use the API. Once you "authorize" the Application to use an API, you can pick which scopes you want to grant to the Application from the list of scopes allowed by the API.

### Set up a user

We are going to use the user it was created when signed up in Oauth0.

Go to User Management > select our user > go to Permissions > click on "Assign Permissions". And choose "rabbitmq" API and grant all permissions.


## Configure RabbitMQ with OAuth0 signing key

From Oauth0 dashboard, go to Settings > List of Valid Keys, and "Copy Signing Certificate" from the **CURRENTLY USED** signing key.

Create `/tmp/certiicate.pem` and paste the certificate.

Then run `openssl x509 -in /tmp/certificate.pem -pubkey -noout > /tmp/public.pem` to extract the public key from the certificate. And paste the public key into `rabbitmq.config`.

Below we have a sample RabbitMQ configuration where we have set the `default_key` identifier that we copied from
Oauth0 and also the public key we extracted from `/tmp/public.pem`.

```
{key_config, [
	{default_key, <<"LQPlyC9P_gOhzMLx7r2Qm">>},
	{signing_keys,
		#{<<"LQPlyC9P_gOhzMLx7r2Qm">> => {pem, <<"-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuELzgXF5ZiEMkA0EnRii
Nf1pck5SkzK4HN6y+Zvy9F2e2soJ/i7acaVX0z5O1Fj2ez0UIe1cwJxurTdlFHQD
MAHD6Mhr5vhY+UEACk9QXp5jbRQwApzEnmDoEuKKVFmTK9Jvm+339kRWz6vv/CqB
cMWSVjp+bnd+XosA8SwKSboQ9Vs4LdJi0fqIOyu2o+FRkf6p5qPMYLndJAKZfwSg
aeCgC2hpBiylBsYBdHQEmawgcUjW+CKAOaMEix/799jRjpXkmUFxZ+H/wbLnu880
/bqJidYlvoJt88skYlzqmAxf/BWhaudVkiqtFNZcr2kwsZk/O+7GNFk4N0/UdE4Y
CwIDAQAB
-----END PUBLIC KEY-----">>}
		 }
	}]
}

```

## Start RabbitMQ

At the moment, we need to run RabbitMQ directly from source:
1. git clone rabbitmq/rabbitmq-server
2. git checkout oidc-integration
3. `gmake run-broker PLUGINS="rabbitmq_management rabbitmq_auth_backend_oauth2" RABBITMQ_CONFIG_FILE=<root folder of the tutorial>/conf/oauth0/rabbitmq.config`


## Quickly verify that set up OAuth0 application correctly

Run the following command from the command line
```
curl curl --request POST \
--url 'https://<copy the domain from the Application settings>/oauth/token' \
--header 'content-type: application/x-www-form-urlencoded' \
--data grant_type=client_credentials \
--data client_id=<copy the client ID field from the Application settings> \
--data client_secret=<copy the client secret field from the Application settings> \
--data audience=rabbitmq
```
