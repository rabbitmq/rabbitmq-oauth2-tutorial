package com.pivotal.cloud.service.messaging;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.cloud.service.common.AmqpServiceInfo;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Iterator;

public class SpringRabbitConnectionFactoryCreator  {

    private RabbitConnectionFactoryCreator amqpConnectionFactoryCreator;

    public SpringRabbitConnectionFactoryCreator(RabbitConnectionFactoryCreator amqpConnectionFactoryCreator) {
        this.amqpConnectionFactoryCreator = amqpConnectionFactoryCreator;
    }

    public ConnectionFactory create(AmqpServiceInfo serviceInfo, RabbitProperties rabbitProperties,
                                    ObjectProvider<ConnectionNameStrategy> connectionNameStrategy) {
        try {
            return this.createSpringConnectionFactory(serviceInfo, rabbitProperties, connectionNameStrategy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CachingConnectionFactory createSpringConnectionFactory(AmqpServiceInfo serviceInfo, RabbitProperties properties,
                                                                   ObjectProvider<ConnectionNameStrategy> connectionNameStrategy) {
        PropertyMapper map = PropertyMapper.get();
        com.rabbitmq.client.ConnectionFactory amqpConnectionFactory = amqpConnectionFactoryCreator.create(serviceInfo, properties);

        CachingConnectionFactory factory = new CachingConnectionFactory(amqpConnectionFactory);
        if (serviceInfo.getUris() != null) {
            factory.setAddresses(getAddresses(serviceInfo));
        }

        map.from(properties::isPublisherConfirms).to(factory::setPublisherConfirms);
        map.from(properties::isPublisherReturns).to(factory::setPublisherReturns);
        RabbitProperties.Cache.Channel channel = properties.getCache().getChannel();
        map.from(channel::getSize).whenNonNull().to(factory::setChannelCacheSize);
        map.from(channel::getCheckoutTimeout).whenNonNull().as(Duration::toMillis)
                .to(factory::setChannelCheckoutTimeout);
        RabbitProperties.Cache.Connection connection = properties.getCache()
                .getConnection();
        map.from(connection::getMode).whenNonNull().to(factory::setCacheMode);
        map.from(connection::getSize).whenNonNull()
                .to(factory::setConnectionCacheSize);
        map.from(connectionNameStrategy::getIfUnique).whenNonNull()
                .to(factory::setConnectionNameStrategy);

        return factory;

    }

    private String getAddresses(AmqpServiceInfo serviceInfo) {
        if (serviceInfo.getUris() == null || serviceInfo.getUris().isEmpty()) {
            return serviceInfo.getUri();
        }

        try {
            StringBuilder addresses = new StringBuilder();

            URI uri;
            for(Iterator var3 = serviceInfo.getUris().iterator(); var3.hasNext(); addresses.append(uri.getHost()).append(':').append(uri.getPort())) {
                String uriString = (String)var3.next();
                uri = new URI(uriString);
                if (addresses.length() > 0) {
                    addresses.append(',');
                }
            }

            return addresses.toString();
        } catch (URISyntaxException var6) {
            throw new IllegalArgumentException("Invalid AMQP URI", var6);
        }
    }
}
