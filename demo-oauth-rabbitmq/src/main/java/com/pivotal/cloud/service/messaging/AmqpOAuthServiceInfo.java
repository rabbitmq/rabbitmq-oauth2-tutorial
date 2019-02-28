package com.pivotal.cloud.service.messaging;

import org.springframework.cloud.CloudException;
import org.springframework.cloud.service.ServiceInfo;
import org.springframework.cloud.service.common.AmqpServiceInfo;
import org.springframework.cloud.util.UriInfo;

import java.util.List;

@ServiceInfo.ServiceLabel("rabbitmq-oauth")
public class AmqpOAuthServiceInfo extends AmqpServiceInfo {

    private OAuthClientInfo clientInfo;

    public AmqpOAuthServiceInfo(String id, String uri, String managementUri, List<String> uris, List<String> managementUris, OAuthClientInfo oauthInfo) {
        super(id, uri, managementUri, uris, managementUris);
        this.clientInfo = oauthInfo;
        validateOAuthClientInfo(oauthInfo);
    }
    public AmqpOAuthServiceInfo(String id, String uri, String managementUri, OAuthClientInfo oauthInfo) throws CloudException {
        this(id, uri, managementUri, null, null, oauthInfo);
    }

    public OAuthClientInfo getOAuthClient() {
        return clientInfo;
    }

    @Override
    protected UriInfo validateAndCleanUriInfo(UriInfo uriInfo) {
        if (uriInfo.getScheme() == null) {
            throw new IllegalArgumentException("Missing scheme in amqp URI: " + uriInfo);
        }

        if (uriInfo.getHost() == null) {
            throw new IllegalArgumentException("Missing authority in amqp URI: " + uriInfo);
        }



        String path = uriInfo.getPath();
        if (path != null && path.indexOf(47) != -1) {
            throw new IllegalArgumentException("Multiple segments in path of amqp URI: " + uriInfo);
        }

        return uriInfo;

    }

    private void validateOAuthClientInfo(OAuthClientInfo oauthInfo) {
        if (useUsernameAndPasswordCredentials()) {
            assertUriHasCredentials();
        }else {
            assertOAuthClientInfo();
        }
    }
    private boolean useUsernameAndPasswordCredentials() {
        return clientInfo == null;
    }

    private void assertUriHasCredentials() {
        if (getUriInfo().getUserName() == null || getUriInfo().getPassword() == null) {
            throw new IllegalArgumentException("Missing username/password from amqp URI: " + getUriInfo());
        }
    }
    private void assertOAuthClientInfo() {
        if (clientInfo.getClientId() == null) {
            throw new IllegalArgumentException("Missing client_id from amqp oauth_client: " + getOAuthClient());
        }
        if (clientInfo.getClientSecret() == null) {
            throw new IllegalArgumentException("Missing client_secret from amqp pauth_client: " + getOAuthClient());
        }
        if (clientInfo.getAuthDomain() == null) {
            throw new IllegalArgumentException("Missing auth_domain from amqp pauth_client: " + getOAuthClient());
        }
    }

}


