# Use https://auth0.com/ as OAuth 2.0 server

We are going to test 3 OAuth flows:
1. Access management ui via a browser
2. Access management rest api
3. Access AMQP protocol

## Prerequisites to follow this guide

- Have an account in https://auth0.com/.
- Docker

## Set up OAuth0

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
