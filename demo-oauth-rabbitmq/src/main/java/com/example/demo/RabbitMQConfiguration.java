package com.example.demo;

import com.pivotal.cloud.service.messaging.AmqpOAuthServiceInfo;
import com.pivotal.cloud.service.messaging.RabbitConnectionFactoryCreator;
import com.pivotal.cloud.service.messaging.SpringRabbitConnectionFactoryCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfiguration {
    private Logger logger = LoggerFactory.getLogger(RabbitMQConfiguration.class);


    @Autowired
    RabbitProperties rabbitProperties;

    @Autowired
    ObjectProvider<ConnectionNameStrategy> connectionNameStrategies;

    @Autowired OAuthConfiguration oauthConfiguration;

    @Bean
    public CloudFactory cloudFactory() {
        CloudFactory cloudFactory = new CloudFactory();
        return cloudFactory;
    }

    @Bean
    public Cloud cloud(CloudFactory factory) {
        return factory.getCloud();
    }

    @Bean SpringRabbitConnectionFactoryCreator rabbitConnectionFactoryCreator() {
       return new SpringRabbitConnectionFactoryCreator(new RabbitConnectionFactoryCreator(oauthConfiguration));
    }

    @Bean
    public org.springframework.amqp.rabbit.connection.ConnectionFactory consumer(
            SpringRabbitConnectionFactoryCreator rabbitConnectionFactoryCreator,
            Cloud cloud) {
        logger.info("Creating consumer Spring ConnectionFactory ...");
        ConnectionFactory factory = rabbitConnectionFactoryCreator.create(
                cloud.getSingletonServiceInfoByType(AmqpOAuthServiceInfo.class),
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
