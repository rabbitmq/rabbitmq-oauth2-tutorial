package com.example.demo.workload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DurableConsumer {
    private Logger logger = LoggerFactory.getLogger(DurableConsumer.class);

    @Value("${durable-consumer.possibleAuthenticationFailureFatal:false}") boolean possibleAuthenticationFailureFatal;
    @Value("${durable-consumer.missingQueuesFatal:false}") boolean missingQueuesFatal;

    @Bean
    public SimpleMessageListenerContainer consumerOnDurableQueue(@Qualifier("durable-consumer.queue") Queue queue,
                                                                 ConnectionFactory connectionFactory) {
        logger.info("Creating consumer on {} ...", queue.getName());

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queue.getName());
        container.setMessageListener(new PlainMessageListener("consumer-durable"));

        // it set to true, it will fail nad not recover if we get access refused
        container.setPossibleAuthenticationFailureFatal(possibleAuthenticationFailureFatal);

        // this is key in order to survive should the hosting queue node disappeared.
        // without this flag=false, the container will simply give up after 3 attempts
        container.setMissingQueuesFatal(missingQueuesFatal);

        return container;
    }
}
