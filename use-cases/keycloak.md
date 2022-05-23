# Use KeyCloak as OAuth 2.0 nserver

## Prerequisites to follow this guide

- Docker
- make

## Deploy Key Cloak

First, deploy **Key Cloak**. It comes preconfigured with all the required scopes, users and clients.
```
make start-keycloak
```
And add JWT [signing key](http://0.0.0.0:8080/admin/master/console/#/realms/test/keys/providers/rsa/66a592ec-8657-4f53-8870-1e1693ff266c) used by **Key Cloak**:
```
docker exec -it rabbitmq rabbitmqctl add_uaa_key Gnl2ZlbRh3rAr6Wymc988_5cY7T5GuePd5dpJlXDJUk --pem-file=conf/public.pem
```
> Do not mind the fact that the command is called `add_uaa_key`, you can read it as `add_jwt_key`

## Start RabbitMQ

Next, launch RabbitMQ with the configuration of your choice as we have learnt from previous use cases.
With the most basic setup, launch it as follows:
```
CONFIG=keycloak/rabbitmq.conf make start-rabbitmq
```
> CONFIG is relative to conf folder


## Access Management api

Access the management api using the client [mgt_api_client](http://0.0.0.0:8080/admin/master/console/#/realms/test/clients/c5be3c24-0c88-4672-a77a-79002fcc9a9d) which has the scope [rabbitmq.tag:administrator](http://0.0.0.0:8080/admin/master/console/#/realms/test/client-scopes/f6e6dd62-22bf-4421-910e-e6070908764c)

```
make curl-keycloak url=http://localhost:15672/api/overview client_id=mgt_api_client secret=LWOuYqJ8gjKg3D2U8CJZDuID3KiRZVDa
```

## Access AMQP protocol

```
make start-perftest-producer-with-token PRODUCER=producer TOKEN=$(bin/keycloak/token producer kbOFBXI9tANgKUq8vXHLhT6YhbivgXxn)
```

## Access Management UI
