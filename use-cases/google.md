# Use Google as OAuth 2.0 server

**Google is not supported**

The main reason is because it does not issue JWT access tokens
but opaques access tokens. In order to support opaque access tokens, RabbitMQ would have to issue an
external HTTP request to convert the opaque access token into a JWT access token.

Under `/conf/google` folder you can find the configuration used to connect the
RabbitMQ management ui with Google OAuth 2.0 endpoints. With this configuration,
you can get to a point where the user is authenticated by Google, and eventually
you get the error message in the RabbitMQ Management UI "Not Authorized".

The reason is because RabbitMQ cannot validate the access token as it is invalid.
