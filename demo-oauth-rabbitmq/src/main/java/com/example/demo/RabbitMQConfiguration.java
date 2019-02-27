package com.example.demo;

import com.pivotal.cloud.service.messaging.RabbitConnectionFactoryCreator;
import com.pivotal.cloud.service.messaging.SpringRabbitConnectionFactoryCreator;
import com.rabbitmq.client.impl.CredentialsProvider;
import com.rabbitmq.client.impl.DefaultCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.common.AmqpServiceInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

import javax.annotation.PostConstruct;


@Configuration
public class RabbitMQConfiguration {
    private Logger logger = LoggerFactory.getLogger(RabbitMQConfiguration.class);


    @Autowired
    RabbitProperties rabbitProperties;

    @Autowired
    ObjectProvider<ConnectionNameStrategy> connectionNameStrategies;


    @Bean
    public CloudFactory cloudFactory() {
        CloudFactory cloudFactory = new CloudFactory();
        return cloudFactory;
    }

    @Bean
    public Cloud cloud(CloudFactory factory) {
        return factory.getCloud();
    }

    @Bean SpringRabbitConnectionFactoryCreator rabbitConnectionFactoryCreator(@Autowired(required = false) OAuthConfiguration oauthConfiguration) {

        return new SpringRabbitConnectionFactoryCreator(new RabbitConnectionFactoryCreator(
                oauthConfiguration != null ? new OauthCredentialProvider(oauthConfiguration) : null
        ));

    }

    @Bean
    public org.springframework.amqp.rabbit.connection.ConnectionFactory consumer(
            SpringRabbitConnectionFactoryCreator rabbitConnectionFactoryCreator,
            Cloud cloud) {
        logger.info("Creating consumer Spring ConnectionFactory ...");
        ConnectionFactory factory = rabbitConnectionFactoryCreator.create(
                cloud.getSingletonServiceInfoByType(AmqpServiceInfo.class),
                rabbitProperties, connectionNameStrategies);

        return factory;
    }


    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory factory) {
        RabbitAdmin admin  = new RabbitAdmin(factory);

        // This is key if we only have just on RabbitAdmin otherwise one
        // failure could cause the rest of the declarations to fail
        admin.setIgnoreDeclarationExceptions(true);

        return admin;
    }


}
class OauthCredentialProvider implements CredentialsProvider {

    private OAuthConfiguration oAuthConfiguration;
    private OAuth2AccessTokenResponse oAuth2AccessTokenResponse;


    public OauthCredentialProvider(OAuthConfiguration oAuthConfiguration) {
        this.oAuthConfiguration = oAuthConfiguration;
    }

    @Override
    public String getUsername() {
        return "";
    }

    @Override
    public String getPassword() {
        if (oAuth2AccessTokenResponse == null) {
            oAuth2AccessTokenResponse = oAuthConfiguration.oauthAccessToken();
            // WHEN DO WE REFRESH IT ?
        }
        return oAuth2AccessTokenResponse.getAccessToken().getTokenValue();
    }
}