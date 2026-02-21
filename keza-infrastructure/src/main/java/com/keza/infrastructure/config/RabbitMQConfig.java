package com.keza.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchanges
    public static final String NOTIFICATION_EXCHANGE = "keza.notification";
    public static final String KYC_EXCHANGE = "keza.kyc";
    public static final String PAYMENT_EXCHANGE = "keza.payment";
    public static final String AI_EXCHANGE = "keza.ai";
    public static final String DUE_DILIGENCE_EXCHANGE = "keza.due-diligence";
    public static final String DLX_EXCHANGE = "keza.dlx";

    // Queues
    public static final String NOTIFICATION_QUEUE = "keza.notification.queue";
    public static final String KYC_PROCESSING_QUEUE = "keza.kyc-processing.queue";
    public static final String PAYMENT_CALLBACK_QUEUE = "keza.payment-callback.queue";
    public static final String AI_PROCESSING_QUEUE = "keza.ai-processing.queue";
    public static final String DUE_DILIGENCE_QUEUE = "keza.due-diligence.queue";
    public static final String INVESTMENT_PAYMENT_QUEUE = "keza.investment-payment.queue";
    public static final String NOTIFICATION_PAYMENT_QUEUE = "keza.notification-payment.queue";

    // DLQ Queues
    public static final String NOTIFICATION_DLQ = "keza.notification.dlq";
    public static final String KYC_PROCESSING_DLQ = "keza.kyc-processing.dlq";
    public static final String PAYMENT_CALLBACK_DLQ = "keza.payment-callback.dlq";
    public static final String AI_PROCESSING_DLQ = "keza.ai-processing.dlq";
    public static final String DUE_DILIGENCE_DLQ = "keza.due-diligence.dlq";
    public static final String INVESTMENT_PAYMENT_DLQ = "keza.investment-payment.dlq";
    public static final String NOTIFICATION_PAYMENT_DLQ = "keza.notification-payment.dlq";

    // Routing Keys
    public static final String NOTIFICATION_ROUTING_KEY = "notification";
    public static final String KYC_ROUTING_KEY = "kyc.processing";
    public static final String PAYMENT_ROUTING_KEY = "payment.callback";
    public static final String AI_ROUTING_KEY = "ai.processing";
    public static final String DUE_DILIGENCE_ROUTING_KEY = "due-diligence";

    // --- Exchanges ---
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public DirectExchange kycExchange() {
        return new DirectExchange(KYC_EXCHANGE);
    }

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public DirectExchange aiExchange() {
        return new DirectExchange(AI_EXCHANGE);
    }

    @Bean
    public DirectExchange dueDiligenceExchange() {
        return new DirectExchange(DUE_DILIGENCE_EXCHANGE);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    // --- Queues with DLX ---
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue kycProcessingQueue() {
        return QueueBuilder.durable(KYC_PROCESSING_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", KYC_PROCESSING_DLQ)
                .build();
    }

    @Bean
    public Queue paymentCallbackQueue() {
        return QueueBuilder.durable(PAYMENT_CALLBACK_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAYMENT_CALLBACK_DLQ)
                .build();
    }

    @Bean
    public Queue aiProcessingQueue() {
        return QueueBuilder.durable(AI_PROCESSING_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", AI_PROCESSING_DLQ)
                .build();
    }

    @Bean
    public Queue dueDiligenceQueue() {
        return QueueBuilder.durable(DUE_DILIGENCE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DUE_DILIGENCE_DLQ)
                .build();
    }

    @Bean
    public Queue investmentPaymentQueue() {
        return QueueBuilder.durable(INVESTMENT_PAYMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", INVESTMENT_PAYMENT_DLQ)
                .build();
    }

    @Bean
    public Queue notificationPaymentQueue() {
        return QueueBuilder.durable(NOTIFICATION_PAYMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_PAYMENT_DLQ)
                .build();
    }

    // --- DLQ Queues ---
    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    @Bean
    public Queue kycProcessingDlq() {
        return QueueBuilder.durable(KYC_PROCESSING_DLQ).build();
    }

    @Bean
    public Queue paymentCallbackDlq() {
        return QueueBuilder.durable(PAYMENT_CALLBACK_DLQ).build();
    }

    @Bean
    public Queue aiProcessingDlq() {
        return QueueBuilder.durable(AI_PROCESSING_DLQ).build();
    }

    @Bean
    public Queue dueDiligenceDlq() {
        return QueueBuilder.durable(DUE_DILIGENCE_DLQ).build();
    }

    @Bean
    public Queue investmentPaymentDlq() {
        return QueueBuilder.durable(INVESTMENT_PAYMENT_DLQ).build();
    }

    @Bean
    public Queue notificationPaymentDlq() {
        return QueueBuilder.durable(NOTIFICATION_PAYMENT_DLQ).build();
    }

    // --- Bindings ---
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(notificationExchange()).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding kycBinding() {
        return BindingBuilder.bind(kycProcessingQueue()).to(kycExchange()).with(KYC_ROUTING_KEY);
    }

    @Bean
    public Binding paymentBinding() {
        return BindingBuilder.bind(paymentCallbackQueue()).to(paymentExchange()).with(PAYMENT_ROUTING_KEY);
    }

    @Bean
    public Binding aiBinding() {
        return BindingBuilder.bind(aiProcessingQueue()).to(aiExchange()).with(AI_ROUTING_KEY);
    }

    @Bean
    public Binding dueDiligenceBinding() {
        return BindingBuilder.bind(dueDiligenceQueue()).to(dueDiligenceExchange()).with(DUE_DILIGENCE_ROUTING_KEY);
    }

    @Bean
    public Binding investmentPaymentBinding() {
        return BindingBuilder.bind(investmentPaymentQueue()).to(paymentExchange()).with(PAYMENT_ROUTING_KEY);
    }

    @Bean
    public Binding notificationPaymentBinding() {
        return BindingBuilder.bind(notificationPaymentQueue()).to(paymentExchange()).with(PAYMENT_ROUTING_KEY);
    }

    // --- DLQ Bindings ---
    @Bean
    public Binding notificationDlqBinding() {
        return BindingBuilder.bind(notificationDlq()).to(dlxExchange()).with(NOTIFICATION_DLQ);
    }

    @Bean
    public Binding kycDlqBinding() {
        return BindingBuilder.bind(kycProcessingDlq()).to(dlxExchange()).with(KYC_PROCESSING_DLQ);
    }

    @Bean
    public Binding paymentDlqBinding() {
        return BindingBuilder.bind(paymentCallbackDlq()).to(dlxExchange()).with(PAYMENT_CALLBACK_DLQ);
    }

    @Bean
    public Binding aiDlqBinding() {
        return BindingBuilder.bind(aiProcessingDlq()).to(dlxExchange()).with(AI_PROCESSING_DLQ);
    }

    @Bean
    public Binding dueDiligenceDlqBinding() {
        return BindingBuilder.bind(dueDiligenceDlq()).to(dlxExchange()).with(DUE_DILIGENCE_DLQ);
    }

    @Bean
    public Binding investmentPaymentDlqBinding() {
        return BindingBuilder.bind(investmentPaymentDlq()).to(dlxExchange()).with(INVESTMENT_PAYMENT_DLQ);
    }

    @Bean
    public Binding notificationPaymentDlqBinding() {
        return BindingBuilder.bind(notificationPaymentDlq()).to(dlxExchange()).with(NOTIFICATION_PAYMENT_DLQ);
    }

    // --- Message Converter ---
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setDefaultRequeueRejected(false);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(10);
        return factory;
    }
}
