package com.pivotal.cloud.service.messaging;

import org.springframework.cloud.cloudfoundry.CloudFoundryServiceInfoCreator;
import org.springframework.cloud.cloudfoundry.Tags;
import org.springframework.cloud.service.common.AmqpServiceInfo;

import java.util.List;
import java.util.Map;

public class AmqpOAuthServiceInfoCreator extends CloudFoundryServiceInfoCreator<AmqpOAuthServiceInfo> {

    public AmqpOAuthServiceInfoCreator() {
        super(new Tags("rabbitmq-oauth"), AmqpServiceInfo.AMQP_SCHEME, AmqpServiceInfo.AMQPS_SCHEME);
    }

    public AmqpOAuthServiceInfo createServiceInfo(Map<String, Object> serviceData) {
        Map<String, Object> credentials = this.getCredentials(serviceData);
        String id = this.getId(serviceData);
        String uri = this.getUriFromCredentials(credentials);
        String managementUri = this.getStringFromCredentials(credentials, new String[]{"http_api_uri"});
        OAuthClientInfo oauthClient = getOAuthClientInfo(credentials);
        if (credentials.containsKey("uris")) {
            List<String> uris = (List) credentials.get("uris");
            List<String> managementUris = (List) credentials.get("http_api_uris");
            return new AmqpOAuthServiceInfo(id, uri, managementUri, uris, managementUris, oauthClient);
        } else {
            return new AmqpOAuthServiceInfo(id, uri, managementUri, oauthClient);
        }
    }

    private OAuthClientInfo getOAuthClientInfo(Map<String, Object> credentials) {
        if (!credentials.containsKey("oauth_client")) {
            return null;
        }
        Map<String, Object> oauth_client = (Map<String, Object>)credentials.get("oauth_client");
        String clientId = (String)oauth_client.get("client_id");
        String client_secret = (String)oauth_client.get("client_secret");
        String oauth_domain = (String)oauth_client.get("auth_domain");

        return new OAuthClientInfo(clientId, client_secret, oauth_domain);
    }
}
