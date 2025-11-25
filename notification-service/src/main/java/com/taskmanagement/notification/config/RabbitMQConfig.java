package com.taskmanagement.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    // Exchange names
    public static final String TASK_EXCHANGE = "task.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    
    // Queue names
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String EMAIL_QUEUE = "email.queue";
    public static final String TASK_EVENTS_QUEUE = "task.events.queue";
    
    // Routing keys
    public static final String TASK_CREATED_KEY = "task.created";
    public static final String TASK_UPDATED_KEY = "task.updated";
    public static final String TASK_COMPLETED_KEY = "task.completed";
    public static final String TASK_ASSIGNED_KEY = "task.assigned";
    public static final String TASK_DUE_REMINDER_KEY = "task.due.reminder";
    public static final String EMAIL_SEND_KEY = "email.send";
    
    // Exchanges
    @Bean
    public TopicExchange taskExchange() {
        return new TopicExchange(TASK_EXCHANGE);
    }
    
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }
    
    // Queues
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_EXCHANGE + ".dlx")
                .build();
    }
    
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_EXCHANGE + ".dlx")
                .build();
    }
    
    @Bean
    public Queue taskEventsQueue() {
        return QueueBuilder.durable(TASK_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", TASK_EXCHANGE + ".dlx")
                .build();
    }
    
    // Dead Letter Exchange and Queue
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE + ".dlx");
    }
    
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE + ".dlq").build();
    }
    
    // Bindings
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with("notification.*");
    }
    
    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue())
                .to(notificationExchange())
                .with(EMAIL_SEND_KEY);
    }
    
    @Bean
    public Binding taskEventsBinding() {
        return BindingBuilder.bind(taskEventsQueue())
                .to(taskExchange())
                .with("task.*");
    }
    
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(NOTIFICATION_QUEUE + ".dlq");
    }
    
    // Message converter
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    // RabbitTemplate
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
    
    // Listener container factory
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}