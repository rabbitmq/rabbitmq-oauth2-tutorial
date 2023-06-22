# Use Azure Active Directory (Azure AD) as OAuth 2.0 server

You are going to test 3 OAuth flows:
1. Access management ui via a browser :ballot_box_with_check:
2. Access management rest api :construction:
3. Access AMQP protocol :construction:

## Prerequisites to follow this guide

- Have an account in https://portal.azure.com.
- Docker
- Openssl

## Register your app

When using **Azure AD as OAuth 2.0 server**, your client app (in our case RabbitMQ) needs a way to trust the security tokens issued to it by the **Microsoft identity platform**. The first step in establishing that trust is by **registering your app** with the identity platform in Azure AD.

> :blue_book: More details about App registration in Azure AD are available [here](https://docs.microsoft.com/en-us/azure/active-directory/develop/quickstart-register-app)

Once you have logged onto your account in [Azure Portal](https://portal.azure.com), go to **Azure Active Directory** (use the search bar if you are not able to easily find it).

In the left-hand navigation menu, click on **App Registrations**. Then, select **New registration**.

In the **Register an application** pane, provide the following informations:

- **Name**: the name you would like to give to your application (ex: *rabbitmq-oauth2*)
- **Supported Account Types**: select **Accounts in this organizational directory only (Default Directory only - Single tenant)** (you can choose other options if you want to enlarge the audience of your app)
- **Redirect URI**:
  - On the **Select a platform** drop-down list, select **Single-page application (SPA)**
  - Configure the **Redirect URI** to: `https://localhost:15671/js/oidc-oauth/login-callback.html`

> :warning: **IMPORTANT**: As you may have noticed, Azure AD only allows `https` links as **Redirect URI**. To fit this need, you will enable HTTPS for RabbitMQ Management UI, on port `15671`.

Click on **Register**.

![Azure AD OAuth2 App](../assets/azure-ad-oauth-registered-app.png)

Note the following values, as you will need it later to configure the `rabbitmq_auth_backend_oauth2` on RabbitMQ side:
- Directory (tenant ID)
- Application (client) ID

Click on the **Endpoints** tab and, on the right pane that has just opened, copy the value of **OpenID Connect metadata document** (ex: `https://login.microsoftonline.com/{TENANT_ID}/v2.0/.well-known/openid-configuration`) and open it in your browser.

Note the value of the `jwks_uri` key (ex: `https://login.microsoftonline.com/{TENANT_ID}/discovery/v2.0/keys`), as you will also need it later to configure the `rabbitmq_auth_backend_oauth2` on RabbitMQ side

![Azure AD JWKS URI](../assets/azure-ad-jwks-uri.png)

## Create a secret for your app

Your application needs a **client secret** to prove its identity when requesting a token.

Still on the **App registrations** page, in the left-hand menu, click on **Certificates & Secrets**, then select the **Client secrets** tab.

In the **Certificates & Secrets** pane, click on **New Client Secret** and, on the right pane that has just opened, enter a description for the secret and choose an expiration time.

Click on **Add**.

> :warning: **IMPORTANT**: Immediately note the value of the secret (as you won't be able to get it later and you will need it to configure the `rabbitmq_auth_backend_oauth2` on RabbitMQ side)

## Create OAuth 2.0 roles for your app

App roles are defined by using the [Azure portal](https://portal.azure.com) during the app registration process. When a user signs in to your application, Azure AD emits a `roles` claim for each role that the user or service principal has been granted (you will have a look at it at the end of this tutorial).

> :blue_book: More details about roles in Azure AD are available [here](https://docs.microsoft.com/en-us/azure/active-directory/develop/howto-add-app-roles-in-azure-ad-apps)

Still in [Azure Portal](https://portal.azure.com), go back to **Azure Active Directory** home page. In the left-hand menu, click on **App Registrations** and then click on your **application name** to open your application **Overview** pane.

### Create a role to allow access to Management UI

In the left-hand menu, click on **App Roles**. Then, click on **Create App Role** to create an OAuth 2.0 role that will be used to give access to the RabbitMQ Management UI.

> :blue_book: More details about how permissions are managed on RabbitMQ when using OAuth2 are available [here](https://github.com/rabbitmq/rabbitmq-oauth2-tutorial#about-permissions).

On the right menu that has just opened, provide the requested information:

- **Display Name**: the name you want to give to the role (ex: *Management UI Admin*)
- **Allowed member types**: Both (Users/Groups + Applications)
- **Value**: `Application_ID.tag:administrator` (where *Application_ID* is the value of the *Application (client) ID* noted earlier in this tutorial)
- **Description**: briefly describe what this role aims to (here just to give admin access to the RabbitMQ Management UI)
- **Do you want to enable this app role**: `yes` (check the box)

Click on **Apply**.

### Create a role to grant configure permission on all resources

Click again on **Create App Role**. You are now going to create an OAuth 2.0 role that will be used to give configure access to all the resources on all the RabbitMQ vhosts.

On the right menu that has just opened, fill the form as below:

- **Display Name**: the name you want to give to the role (ex: *Configure All Vhosts*)
- **Allowed member types**: Both (Users/Groups + Applications)
- **Value**: `Application_ID.configure:*/*` (where *Application_ID* is the value of the *Application (client) ID* noted earlier in this tutorial)
- **Description**: briefly describe what this role aims to (here to give permissions to configure all resources on all the vhosts available on the RabbitMQ instance)
- **Do you want to enable this app role**: `yes` (check the box)

Click on **Apply**.

## Assign App Roles to users

Now that some roles have been created for your application, you still need to assign these to some users.

Still in [Azure Portal](https://portal.azure.com), go back to **Azure Active Directory** home page and, in the left-hand menu, click on **Enterprise Applications**.

In the new left-hand menu, select **Manage -> All applications**. Use the **Search Bar** and/or the available filters to find your application.

![Azure AD Enterprise Applications](../assets/azure-ad-enterprise-application.png)

Click on the application you just created, for which you want to assign roles to users/groups, then, in the left-hand navigation menu, Select **Manage -> Users and groups**.

Click on **Add user/group** to open the **Add Assignment** pane.

Below **Users**, click on *None Selected* and, on the **Users** pane that has just opened on the right, search and select the users/groups you want to assign roles to.

Once you've selected users and groups, click on the **Select** button.

Back to the **Add assignment** pane, below **Select a Role**, click on *None Selected* and, on the **Select a role** pane that has just opened on the right, search and select the role you want to assign to the selected users.

> :bulb: If only one role is available for your application, it would be automatically selected and greyed by default

Choose a role (only a single role can be selected at a time), click on the **Select** button, and click on the **Assign** button to finalize the assignment of users and groups to the app.

:repeat: Repeat the operations for all the roles you want to assign.


## Create Custom Signing Keys

It is optional to create a signing key for your application. If you create one though, you must append an `appid` query parameter containing the *app ID* to the `jwks_uri`. Otherwise, the standard jwks_uri endpoint will not include the custom signing key and RabbitMQ will not find the signing key to validate the token's signature.

For example, given your application id, `{my-app-id}` and your tenant `{tenant}`, the OIDC discovery endpoint uri would be `https://login.microsoftonline.com/{tenant}/.well-known/openid-configuration?appid={my-app-id}`. The returned payload contains the `jwks_uri` attribute whose value is something like `https://login.microsoftonline.com/{tenant}/discovery/keys?appid=<my-app-idp>`. RabbitMQ should be configured with that `jwks_uri` value.

## Configure RabbitMQ to use Azure AD as OAuth 2.0 authentication backend

The configuration on Azure side is done. You now have to configure RabbitMQ to use the resources you just created.

[rabbitmq.config](../conf/azure/rabbitmq.config) is a sample RabbitMQ advanced configuration to **enable Azure AD as OAuth 2.0 authentication backend** for the RabbitMQ Management UI.

Update it with the following values (you should have noted these in the previous steps):
- **Tenant ID** associated to the app that you registered in Azure AD
- **Application ID** associated to the app that you registered in Azure AD
- Value of the **secret** you created for our app in Azure AD
- Value of the **jwks_uri** key from `https://login.microsoftonline.com/{TENANT_ID}/v2.0/.well-known/openid-configuration`

```
$ vi rabbitmq.config
[
  {rabbit, [
   {auth_backends, [rabbit_auth_backend_oauth2, rabbit_auth_backend_internal]}
  ]},
  {rabbitmq_management, [
     {oauth_enabled, true},
     {oauth_client_id, "PUT YOUR AZURE AD APPLICATION ID"},
     {oauth_client_secret, "PUT YOUR AZURE AD APPLICATION SECRET"},
     {oauth_provider_url, "https://login.microsoftonline.com/AZURE_AD_TENANT_ID"}
 ]},
 {rabbitmq_auth_backend_oauth2, [
   {resource_server_id, <<"PUT YOUR AZURE AD APPLICATION ID">>},
   {extra_scopes_source, <<"roles">>},
   {key_config, [
     {jwks_url, <<"PUT YOUR AZURE AD JWKS URI VALUE">>}
   ]}
 ]}
].
```

> :warning: Please update the file available in this tutorial ([here](../conf/azure/rabbitmq.config)), as it will be automatically loaded in the RabbitMQ instance that you are going to deploy later in this tutorial

### Generate SSL certificate and key
> :warning: Remember when you have registered your app on Azure AD that it only allows **https** protocol for OAuth2 **Redirect URI**? You will thus need to enable HTTPS for RabbitMQ Management UI amd its underlying API.

For the purpose of this tutorial, you can generate a self-signed certificate/key pair.

Run the following command (depending on your config, you may have to be root):
```
make build-azure
```

This generates the following files in `conf/azure`:
- **rabbitmq-ca.**crt**: a custom certificate authority that is used to generate and sign a self signed certificate for RabbitMQ
- **rabbitmq.crt**: a self-signed certificate (cn=localhost)
- **rabbitmq.key**: the private key associated to the `rabbitmq.crt` certificate

:arrow_right: These files will be mounted into the `rabbitmq` docker container in the next steps of this tutorial, where they will be used to configure HTTPS for the RabbitMQ Management UI/API

## Start RabbitMQ

Run the following commands to run RabbitMQ docker image:

```
export MODE=azure
make start-rabbitmq
```
:arrow_right: This starts a docker container named `rabbitmq`, with RabbitMQ Management UI/API with HTTPS enabled, and configured to use your Azure AD as OAuth2 Authentication Backend, based on the information you provided in `rabbitmq.conf` in the previous steps of this tutorial. There is also a `rabbitmq.config` but it is there just for reference should you wanted to use the old style. 

## Verify RabbitMQ Management UI access

Go to RabbitMQ Management UI `https://localhost:15671`. Depending on your browser, ignore the security warnings (raised by the fact that you are using a self-signed certificate) to proceed.

Once on the RabbitMQ Management UI page, click on the **Click here to log in** button,
authenticate with your **Azure AD user**. The first time, you are likely going to have to give your
consent (it depends on the policies applied to Azure AD on your side).

> :warning: At first login, you may face the `AADSTS90008` error: just click on **Click here to log in** button
again and it will disappear (this issue seems to be known, as illustrated [here](https://docs.microsoft.com/en-us/answers/questions/671457/after-34accept34-on-consent-prompt-on-azure-sso-lo.html#answer-893848))

At the end, you should be redirected back to the RabbitMQ Management UI.

Azure AD issues an access token like this one below. The permissions are managed in the `roles` claim.
You have configured RabbitMQ with `{extra_scopes_source, <<"roles">>},` which means RabbitMQ uses
the scopes in the `roles` claim to define permissions for a logged-in user.

```
{
  "aud": "30b61ef8-72d7-4e40-88f2-6e16c8d3fd88",
  "iss": "https://sts.windows.net/1ffc6121-590e-4aa5-bf47-c348674069cb/",
  "iat": 1655740039,
  "nbf": 1655740039,
  "exp": 1655744211,
  "acr": "1",
  "aio": "AUQAu/8TAAAAjvwucwL4nZe83vNZvg6A7sAPscI9zsGvRs8EuT7aVhubpmhRnxJ+X7nbkISoP5eBBMxoi2yiCclnH2Ocjjzsqw==",
  "amr": [
    "wia"
  ],
  "appid": "30b61ef8-72d7-4e40-88f2-6e16c8d3fd88",
  "appidacr": "1",
  "email": "baptiste.daroit@company.com",
  "idp": "https://sts.windows.net/b3f4f7c2-72ce-4192-aba4-d6c7719b5766/",
  "in_corp": "true",
  "ipaddr": "xxx.xxx.xxx.xxx",
  "name": "Baptiste DA ROIT",
  "oid": "cf2df3b4-03df-4e1e-b5c0-f232932aaead",
  "rh": "0.AR8AgCG80x7L90C1mhVBBXQzQjgoklctsdBMtgYVWFwc4tgfAMQ.",
  "roles": [
    "30b61ef8-72d7-4e40-88f2-6e16c8d3fd88.tag:monitoring",
    "30b61ef8-72d7-4e40-88f2-6e16c8d3fd88.configure:*/*"
  ],
  "scp": "User.Read",
  "sub": "6aBzW3a1FOTTrnlZEuC1SmwG0sRjVgQU49DvrYK6Rqg",
  "tid": "1ffc6121-590e-4aa5-bf47-c348674069cb",
  "unique_name": "baptiste.daroit@company.com",
  "uti": "QHqwThTqQEK9iMdnRuD_AA",
  "ver": "1.0"
}
```
