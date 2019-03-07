package com.pivotal.cloud.service.messaging;

public class OAuthClientInfo {
    private String clientId;
    private String clientSecret;
    private String authDomain;

    public OAuthClientInfo(String clientId, String clientSecret, String authDomain) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authDomain = authDomain;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getAuthDomain() {
        return authDomain;
    }

    @Override
    public String toString() {
        return "OAuthClientInfo{" +
                "clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", authDomain='" + authDomain + '\'' +
                '}';
    }
}
