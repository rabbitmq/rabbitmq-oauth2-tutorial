package com.example.demo;

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
    private ClientRegistration oauthClient;

    @Autowired
    private DefaultClientCredentialsTokenResponseClient tokenRequester;

	@Bean
    ClientRegistration oauthClient() {
        return ClientRegistration.withRegistrationId("producer")
                .clientId("producer")
                .clientSecret("producer_secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.POST)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://localhost:8080/uaa/oauth/token")
                .clientName("producer")
                .build();
    }

    public OAuth2AccessTokenResponse oauthAccessToken() {
        OAuth2ClientCredentialsGrantRequest tokenRequest = new OAuth2ClientCredentialsGrantRequest(oauthClient);
        return tokenRequester.getTokenResponse(tokenRequest);
    }

    @Bean
    DefaultClientCredentialsTokenResponseClient defaultClientCredentialsTokenResponseClient() {
	    return new DefaultClientCredentialsTokenResponseClient();
    }
}
