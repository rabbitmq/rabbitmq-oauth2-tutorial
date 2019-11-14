# Integrating with Single SignOn for PCF service

Applications deployed to Cloud Foundry could get their *OAuth client credentials* by binding to a *Single SignOn Service instance*. Once an application has its *client credentials* it can get an *Oauth access token* and with it connect to RabbitMQ.

How the application, or its *Oauth client*, gets granted access to the requested authorities/scopes is yet to be explored.

**Single Sign-On (SSO) is a multi-tenant service, which enables a deployment to host multiple tenants as service plans. Each service plan can have its own administrators, applications and users. This lets enterprises segregate access by using separate plans.** For info https://docs.pivotal.io/p-identity/1-8/manage-service-plans.html

## Access PCF UAA server

```
uaac target uaa.YOUR-SYSTEM-DOMAIN
```

## Deploy Single SignOn product to your PCF Foundation

The steps below install `1.8.0` and `97.65` ubuntu *xenial stemcell* for GCP.
```
pivnet login --api-token="Login to your pivnet account @ http://run.pivotal.io/ and get your token"
pivnet download-product-files -p pivotal_single_sign-on_service -r 1.8.0  -g "*Single*"
pivnet  download-product-files  -p stemcells-ubuntu-xenial -r 97.65 -g "*google*"
$(smith om)
om -k  upload-product -p Pivotal_Single_Sign-On_Service_1.8.0.pivotal
om -k  upload-stemcell -s light-bosh-stemcell-97.65-google-kvm-ubuntu-xenial-go_agent.tgz
```

## Get developer environment ready to deploy apps

Login to Cloud Foundry as administrator and create an org/space and developer user.

```
cf create-org test
cf create-space test -o test

cf create-user dev dev
cf set-space-role dev test test SpaceDeveloper
```

Login as developer user.

```
cf login -u dev -p dev
```

## Configure Single Sign-On Service

At this point, the *marketplace* does not have yet the Single Sign-One service. We need to enable it to the organizations.

1. Go to `https://p-identity.YOUR-SYSTEM-DOMAIN` (e.g. `p-identity.sys.lightgrayishmagenta.cf-app.com`),
2. Login as UAA administrator. Check [this section](https://docs.pivotal.io/p-identity/1-7/system-plan.html#configure) in the official docs for more details.
3. Add new plan and enable it for `test` organization we created earlier.
  ![SSO Ops dashboard](assets/sso-ops-dashboard.png)
  > We are not going to add any user as administrator of this plan. Instead, we are going to use the same administrator used we used earlier

Our developer user, `dev`, can see the new plan:
```bash
cf marketplace
```

```
Getting services from marketplace in org test / space test as dev...
OK

service      plans                                         description                                                                                                broker
p-rabbitmq   standard                                      RabbitMQ service to provide shared instances of this high-performance multi-protocol messaging broker.     p-rabbitmq
p.rabbitmq   single-node, plan-2, plan-3, plan-4, plan-5   RabbitMQ service to provide dedicated instances of this high-performance multi-protocol messaging broker   rabbitmq-odb
p-identity   test-sso-plan                                 Provides identity capabilities via UAA as a Service                                                        identity-service-broker

TIP: Use 'cf marketplace -s SERVICE' to view descriptions of individual plans of a given service.
```

## Create a Single Sign-On service instance

```bash
cf create-service p-identity test-sso-plan sso
cf service
```

```
Showing info of service sso in org test / space test as dev...

name:             sso
service:          p-identity
tags:
plan:             test-sso-plan
description:      Provides identity capabilities via UAA as a Service
documentation:    https://docs.pivotal.io/p-identity/index.html
dashboard:        https://p-identity.sys.lightgrayishmagenta.cf-app.com/dashboard/identity-zones/bd5c931d-1925-45b4-a20d-f134e975ddec/instances/d434f715-d8bd-4784-a567-64d359c867fb/
service broker:   identity-service-broker

Showing status of last operation from service sso...

status:    create succeeded
message:
started:   2019-03-11T13:30:06Z
updated:   2019-03-11T13:30:06Z

There are no bound apps for this service.
```

We can access the SSO dashboard as either an CF administrator or a CF space developer. A newly created service instance will show an empty list of apps and resources.


## Bind application to SSO service instance to get an OAuth client

Our application will not use RabbitMQ on behalf an end-user identity. Instead, it will use it on its own behalf. Hence, our application needs the OAuth client credential grant. According to the SSO Service, this is a [Service-to-Service](https://docs.pivotal.io/p-identity/1-7/configure-apps/service-to-service-app.html) SSO application Type which maps the *client credentials* OAuth Grant type.

When we bind an application, we tell *SSO service instance* :
- `grant_types` - the OAuth grant type, which is *client credentials*
- `authorities` - ??

> We will use for now a dummy app so that we can grant it a Oauth client
> mkdir myapp
> echo "<html><body><h1>Hello world!</h1></body></html>"  > myapp/index.html
> touch myapp/Staticfile
> cf push myapp -p myapp


### Specify authorities during bind operation

Edit a json file (`bindings-1.json`) with the following content:
```
{
  "grant_types": ["client_credentials"],
  "authorities": ["rabbitmq.read:*/*", "rabbitmq.tag:administrator"]
}

```

```bash
cf bind-service myapp sso -c bindings-1.json
cf env myapp
```

```
{
 "VCAP_SERVICES": {
  "p-identity": [
   {
    "binding_name": null,
    "credentials": {
     "auth_domain": "https://test-sso-plan.login.sys.lightgrayishmagenta.cf-app.com",
     "client_id": "56e9a83d-362d-4883-9cbd-8a00751166d2",
     "client_secret": "57549429-5089-415f-b315-a2da22a0532b"
    },
    "instance_name": "sso",
    "label": "p-identity",
    "name": "sso",
    "plan": "test-sso-plan",
    "provider": null,
    "syslog_drain_url": null,
    "tags": [],
    "volume_mounts": []
   }
  ]
 }
}
```

Get an access token as if we were the application:
```bash
uaac target https://test-sso-plan.login.sys.lightgrayishmagenta.cf-app.com
uaac token client get 56e9a83d-362d-4883-9cbd-8a00751166d2 -s 57549429-5089-415f-b315-a2da22a0532b
uaac context 56e9a83d-362d-4883-9cbd-8a00751166d2
uaac token decode <token>
```

```
jti: 648c14e8d4194e90abd2619674e7bd4c
sub: 56e9a83d-362d-4883-9cbd-8a00751166d2
authorities: uaa.none
scope: uaa.none
client_id: 56e9a83d-362d-4883-9cbd-8a00751166d2
cid: 56e9a83d-362d-4883-9cbd-8a00751166d2
azp: 56e9a83d-362d-4883-9cbd-8a00751166d2
grant_type: client_credentials
rev_sig: 62ee962b
iat: 1552313462
exp: 1552356662
iss: https://test-sso-plan.uaa.sys.lightgrayishmagenta.cf-app.com/oauth/token
zid: bd5c931d-1925-45b4-a20d-f134e975ddec
aud: 56e9a83d-362d-4883-9cbd-8a00751166d2
```

Go to SSO dashboard as `dev` user, we can see our application bound to the service.
![myapp bound to the SSO SI](assets/sso-dashboard-dev-user-after-bind-apps.png)

We can check up front which scopes the application will get via UAAC following these steps:
1. As administrator, create an app directly on the SSO SI dashboard following the steps described [here](https://docs.pivotal.io/p-identity/1-7/manage-clients-api.html#creating)
2. Once we are logged in UAAC we can run this command:
  ```
  uaac client get 56e9a83d-362d-4883-9cbd-8a00751166d2  
    scope: uaa.none
    client_id: 56e9a83d-362d-4883-9cbd-8a00751166d2
    resource_ids: none
    authorized_grant_types: client_credentials
    redirect_uri: https://myapp.apps.lightgrayishmagenta.cf-app.com
    autoapprove:
    authorities: uaa.none
    allowedproviders: uaa
    name: myapp
    type: SERVICE_TO_SERVICE
    space_guid: 6e01dfcb-3010-413e-baaf-b6f93b8210c8
    cf_app_guid: 487996e5-1b45-4a76-badd-b06cc9e4b066
    lastmodified: 1552313212000
  ```


## Create an internal SSO App to access UAA

client_id: c575f5cf-30a6-40ea-a3cf-1441916aac33
client_secret: 6470bf92-fc73-444a-92b6-1077edf76df1


1. *SpaceDeveloper* `dev` creates an SSO service instance
2.


# spike - definining OAuth resources on a separate space so that developers cannot self grant them

1. Login to Cloud Foundry as administrator and create an org/space and developer user.

```
cf create-org test
cf create-space resources -o test
cf create-space test -o test

cf create-user dev dev
cf set-space-role dev test test SpaceDeveloper
```

Login as developer user.

```
cf login -u dev -p dev
```

2. Create an SSO plan (`test-sso-plan`) enabled for `test` organization

3. *SpaceDeveloper* `dev` creates an SSO service instance
  ```
  cf login -u dev -p dev
  cf create-service p-identity test-sso-plan sso
  cf service sso
  ```
4. *UAA* `admin` access the sso dashboard for the service instance created by `dev` user and creates the `rabbitmq-1` with some permissions
  > In SSO tile, tenancy is defined at the service plan. In other words, all the services instances
  > we create from a service plan are all within the same sso tenant.

5. *SpaceDeveloper* `dev` accessed the sso dashboard and creates an app and is able to grant itself access to the resource `rabbitmq-1` created by the *UAA* `admin` user. Ummm.. not great.

6. Login to Cloud Foundry as administrator and create an SSO service instance from the same `test-sso-plan`
