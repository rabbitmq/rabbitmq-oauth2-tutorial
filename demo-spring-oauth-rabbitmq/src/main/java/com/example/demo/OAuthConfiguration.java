package com.example.demo;

import com.pivotal.cloud.service.messaging.OAuthClientInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

@Configuration
public class OAuthConfiguration {


    @Autowired
    private DefaultClientCredentialsTokenResponseClient tokenRequester;

    private ClientRegistration oauthClient(OAuthClientInfo client) {
        return ClientRegistration.withRegistrationId(client.getClientId())
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.POST)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri(String.format("%s/oauth/token", client.getAuthDomain()))
                .build();
    }

    public OAuth2AccessTokenResponse oauthAccessToken(OAuthClientInfo client) {
        ClientRegistration clientRegistration = oauthClient(client);
        OAuth2ClientCredentialsGrantRequest tokenRequest = new OAuth2ClientCredentialsGrantRequest(clientRegistration);
        return tokenRequester.getTokenResponse(tokenRequest);
    }

    @Bean
    DefaultClientCredentialsTokenResponseClient defaultClientCredentialsTokenResponseClient() {
	    return new DefaultClientCredentialsTokenResponseClient();
    }
}
