package de.shopeasy.returnsmanagement.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Legt Exchanges, Queues und Bindings in RabbitMQ an
@Configuration
public class RabbitMqConfig {

    // Namen zentral definieren, damit Producer und Consumer dieselben verwenden
    public static final String STATUS_EXCHANGE = "returns.status";
    public static final String DEAD_LETTER_EXCHANGE = "returns.status.dlx";

    public static final String STATUS_UPDATER_QUEUE = "status-updater";
    public static final String CUSTOMER_NOTIFICATION_QUEUE = "customer-notification";
    public static final String DEAD_LETTER_QUEUE = "status-dead-letter";

    // Präfix für Routing-Keys, z. B. "return.status.approved"
    public static final String ROUTING_KEY_PREFIX = "return.status.";

    // Topic-Exchange: verteilt Statusänderungen anhand des Routing-Keys
    @Bean
    public TopicExchange statusExchange() {
        return new TopicExchange(STATUS_EXCHANGE);
    }

    // Dead-Letter-Exchange: nimmt nicht verarbeitbare Nachrichten auf
    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DEAD_LETTER_EXCHANGE);
    }

    // Queue für den Consumer, der den Retourenstatus aktualisiert.
    // Fehlgeschlagene Nachrichten werden an die Dead-Letter-Exchange weitergeleitet.
    @Bean
    public Queue statusUpdaterQueue() {
        return QueueBuilder.durable(STATUS_UPDATER_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .build();
    }

    // Queue für die Kundenbenachrichtigung
    @Bean
    public Queue customerNotificationQueue() {
        return QueueBuilder.durable(CUSTOMER_NOTIFICATION_QUEUE).build();
    }

    // Queue für nicht verarbeitbare Nachrichten
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    // status-updater erhält alle Statusänderungen
    @Bean
    public Binding statusUpdaterBinding() {
        return BindingBuilder.bind(statusUpdaterQueue())
                .to(statusExchange())
                .with(ROUTING_KEY_PREFIX + "#");
    }

    // customer-notification erhält nur kundenrelevante Statusänderungen
    @Bean
    public Binding labelCreatedNotificationBinding() {
        return BindingBuilder.bind(customerNotificationQueue())
                .to(statusExchange())
                .with(ROUTING_KEY_PREFIX + "label_created");
    }

    @Bean
    public Binding rejectedNotificationBinding() {
        return BindingBuilder.bind(customerNotificationQueue())
                .to(statusExchange())
                .with(ROUTING_KEY_PREFIX + "rejected");
    }

    @Bean
    public Binding refundedNotificationBinding() {
        return BindingBuilder.bind(customerNotificationQueue())
                .to(statusExchange())
                .with(ROUTING_KEY_PREFIX + "refunded");
    }

    // Dead-Letter-Queue an die Dead-Letter-Exchange binden
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange());
    }

    // Nachrichten als JSON statt als Java-Binärformat übertragen
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}