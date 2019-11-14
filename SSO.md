# Centralized Identity Management and Access Control on Pre-Provision RabbitMQ for PCF

> Concern: Every time a user authenticates with UAA in a federated IM setup (i.e. with LDAP), UAA updates the user's scopes in its internal database. I am not sure if this is done synchronously or asynchronously and the impact this may have when we have thousands of AI. Obviously not all AIs will be authenticating at the same time though. But it is worth keeping this in mind.

## Purpose of this guide

This purpose of this document is to guide Operators and Developers on how to centralize identity management and access control outside RabbitMQ. We are focusing only on Pre-Provision RabbitMQ for PCF. We will dedicate another document to address On-Demand RabbitMQ for PCF.

## Centralized vs Non-Centralized Identity Management

By default, RabbitMQ for PCF is the central location for identity and access control management. RabbitMQ is configured with a default *auth backend* which uses an internal database.

In an enterprise world, identities -end users or service accounts (apps)- are centrally managed in a directory service like LDAP or via a token-based service like Kerberos or OAuth. In this world, user administrators manage identifies and their roles from a single point as opposed to having to go to each backend/service like RabbitMQ to do it.

So that we understand the benefits of centrally managing the identities outside of RabbitMQ lets go thru the typical operations we need to carry out:
- An *administrator* creates a RabbitMQ management user that will be responsible of monitoring the health of the cluster.
- An *administrator* changes the permissions to an application so that it can consume from a given queue.
- An *administrator* changes a users's password

With a centralized identity management, the *administrator* would perform the above actions from outside RabbitMQ, say in an LDAP server. Whereas in a non-centralized identity management, the *administrator* has to go to the RabbitMQ management UI or via its REST-api to do it. By the way, in an enterprise world, the *administrator* could be a persona who is solely in charge of managing users. However, in RabbitMQ that user corresponds to the full administrator access. There is no way to limit access to only user administrator.

## Centralized Identity Management solutions   

RabbitMQ distinguishes two types of access based on the nature of the user:
  - End-user/browser-based which access RabbitMQ Management UI (web-based)
  - Backend/protocol-based which access RabbitMQ via one of the supported messaging protocols (AMQP, MQTT, STOMP)

For browser-based users, Single Sign-on is definitely an advantage to have. Specially when it comes to On-Demand RabbitMQ because once a user logs in, s/he can access any RabbitMQ cluster they are entitled to. Whereas in a non-SSO setup, s/he would have log in to each RabbitMQ cluster.

These are the possible solutions for centralized IM and Access control:
  > For solutions which does support SSO, specially for browser-based users, they will be tagged as [SSO]
  > IM user is the persona responsible for managing identities in the Identity Management solution, be it LDAP, or UAA, or others.


- Semi-Centralized IM in LDAP server with RabbitMQ delegating authz to LDAP server
  * End-users are centrally managed in LDAP whereas applications are still managed in RabbitMQ
  * IM user is responsible for creating end-users in LDAP  
  * RabbitMQ for PCF is responsible for creating applications' identities in RabbitMQ
  * RabbitMQ administrator user can later applications' permissions via the RabbitMQ management ui/api
  * RabbitMQ would be configured with 2 auth backends: First LDAP then internal
  * RabbitMQ administrator user is the only user allowed to manage users/permissions. There is no way to separate full administration from user management.
  * Currently supported

- Centralized IM in LDAP server with RabbitMQ delegating authz to LDAP server
  * Both end-users and application are centrally managed in LDAP servers
  * IM user is responsible for creating end-users and applications in LDAP
  * Developer is responsible for configuring the application with its credentials (username+password) which RabbitMQ would check in LDAP when it connects
  * Not-fully supported:
    - RabbitMQ for PCF will create a user in RabbitMQ regardless whether it is configured with LDAP as the only auth backend. There is no way to disable this mechanism today.
    - Applications would need to read the credentials from a "non-official" location. RabbitMQ for PCF shares the credentials via a service in the VCAP_SERVICES with the tag `rabbitmq`. A developer could leverage User-provided-Services to partially mimic this mechanism. I say partially because with UPS we cannot define tags which means we have to the change the applications' code to read the credentials in other way.

- Centralize IM in UAA server with RabbitMQ delegating authz to OAuth protocol. Applications uses Client Credential Oauth grant type [SSO]
  * Both end-users and applications are centrally managed in UAA server (users; clients; and groups are all stored in UAA's database. Furthermore, UAA exposes SCIM specification for user administration)
  * IM user is responsible for creating end-users in UAA. This can be done in many way:
    - UAAC
    - UAA's rest-api
    - UAA's SCIM api
    - If the user is in the same identity zone as PCF, a CF administrator can create it via cf cli too or send an invitation
  * An application needs an OAuth client in UAA and its credentials (i.e.`{client_id, client_secret}`). The most secure way to do it is by leveraging the SSO service. An application binds to it in order to get an OAuth client in UAA and its credentials passed via VCAP_SERVICES.
  * Not supported yet if we wanted to an IM user ultimately decide what scopes an application has otherwise it is fully supported (the application would get access to any resource declared on the developer's space)  
    - RabbitMQ for PCF would need to declare the service instance as an OAuth Resource in a dedicated org and space where space developers do not have access
    - A IM user has to manually grant/revoke scopes to/from an application via the SSO dashboard

- Centralize IM in UAA server for end-users only and in RabbitMQ for applications [SSO]
  * Management users would be centrally managed in UAA and they leverage SSO
  * Whereas applications would be in RabbitMQ internal database
  

## Non-Federated SSO

The first architecture we are going to explore centrally manages sole the identities at UAA. We call it *non-federated* because all the identities (end-users and clients or service accounts) are stored in UAA internal database.

![Non-Federated SSO](assets/non-federated-sso.png)  

**User** and **OAuth clients** are stored in the Internal Identity provider in UAA (which ultimately relies on a RDBMS).
