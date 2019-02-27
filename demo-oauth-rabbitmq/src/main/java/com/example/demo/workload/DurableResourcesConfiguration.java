package com.example.demo.workload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DurableResourcesConfiguration {

    private Logger logger = LoggerFactory.getLogger(DurableResourcesConfiguration.class);

    @Value("${durable-consumer.queue:durable-q}") String queueName;
    @Value("${durable-consumer.directExchange:durable-e}") String exchangeName;
    @Value("${durable-consumer.routingKey:durable-q}") String routingKey;

    @Bean("durable-consumer.queue")
    public Queue consumerQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean("durable-consumer.directExchange")
    public Exchange consumerExchange() {
        return ExchangeBuilder.directExchange(exchangeName).build();
    }

    @Bean("durable-consumer.binding")
    public Binding consumerBinding() {
        return new Binding(queueName, Binding.DestinationType.QUEUE, exchangeName, routingKey, null);
    }

    @Bean
    public RabbitTemplate templateForDurableProducer(ConnectionFactory connectionFactory) {
        logger.info("Creating templateForDurableProducer ...");

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setRoutingKey(routingKey);
        template.setExchange(exchangeName);

        return template;
    }


}

class PlainMessageListener implements MessageListener {

    private Logger logger = LoggerFactory.getLogger(PlainMessageListener.class);

    private String name;
    private long receivedMessageCount;
    private long failedMessageCount;


    public PlainMessageListener(String name) {
        this.name = name;
    }
    public PlainMessageListener() {
        this("");
    }


    @Override
    public void onMessage(Message message) {

        logger.info("{}/{} received (#{}/#{}) from {}/{} ",
                name,
                Thread.currentThread().getId(),
                receivedMessageCount,
                failedMessageCount,
                message.getMessageProperties().getConsumerQueue(),
                message.getMessageProperties().getConsumerTag()

        );
        receivedMessageCount++;
    }
}