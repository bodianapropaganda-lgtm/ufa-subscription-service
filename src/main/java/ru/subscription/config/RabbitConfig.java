package ru.subscription.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    TopicExchange subscriptionEventsExchange() {
        return new TopicExchange("subscription.events", true, false);
    }

    @Bean
    Queue cacheQueue() {
        return QueueBuilder.durable("subscription.cache")
                .deadLetterExchange("subscription.cache.dlx")
                .deadLetterRoutingKey("subscription.cache.failed")
                .build();
    }

    @Bean
    Binding cacheBinding(Queue cacheQueue, TopicExchange subscriptionEventsExchange) {
        return BindingBuilder.bind(cacheQueue).to(subscriptionEventsExchange).with("#");
    }

    @Bean
    DirectExchange cacheDeadLetterExchange() {
        return new DirectExchange("subscription.cache.dlx", true, false);
    }

    @Bean
    Queue cacheDeadLetterQueue() {
        return QueueBuilder.durable("subscription.cache.dlq").build();
    }

    @Bean
    Binding cacheDeadLetterBinding(
            Queue cacheDeadLetterQueue,
            DirectExchange cacheDeadLetterExchange
    ) {
        return BindingBuilder
                .bind(cacheDeadLetterQueue)
                .to(cacheDeadLetterExchange)
                .with("subscription.cache.failed");
    }
}
