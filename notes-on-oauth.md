
There are two ways of configuring the Idp's endpoints for authentication, token claim,
logout, renew token etc. Without these endpoints, RabbitMQ would not be able to
engage with the Oauth2 protocol. The two ways are:
- manually configuring all the endpoints via the metadata service of the oidc-client-ts library
- or use [OpenID Connect discovery](https://openid.net/specs/openid-connect-discovery-1_0.html)
which is one endpoint we need to configure and that RabbitMQ uses to discover the rest of the
Idp's endpoints.


STS - A token service, or security token service (STS) issues and verifies tokens.
It is one of the endpoints an Idp exposes and which are documented via the OpenIDC discovery endpoint.
An STS issues two kind of tokens: an access token and a refresh token.

RabbitMQ exposes two OAuth2 endpoints. One which where the Idp redirects the user with a code.
And a second one where the Idp redirects the user to complete the logout process.

When the authorization code flow completes, RabbitMq has two tokens, the id-token which is
not used and the access-token which is what RabbitMQ uses to extract user's identity and
the scopes. 
