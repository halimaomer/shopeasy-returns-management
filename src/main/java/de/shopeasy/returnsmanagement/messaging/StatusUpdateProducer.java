package de.shopeasy.returnsmanagement.messaging;

import java.util.Locale;
import java.util.Objects;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

// Verschickt Statusänderungen von Retouren an die Exchange "returns.status".
// Der Producer kennt nur die Exchange, nicht die Queues oder Consumer.
@Component
public class StatusUpdateProducer {

    private final RabbitTemplate rabbitTemplate;

    public StatusUpdateProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // Schickt eine Statusänderung ab.
    // Der Routing-Key wird aus dem Status gebildet, z. B. "return.status.approved".
    public void send(StatusUpdateMessage message) {
        Objects.requireNonNull(message.newStatus(), "Der neue Status darf nicht leer sein");

        String routingKey = RabbitMqConfig.ROUTING_KEY_PREFIX
                + message.newStatus().trim().toLowerCase(Locale.ROOT);

        rabbitTemplate.convertAndSend(RabbitMqConfig.STATUS_EXCHANGE, routingKey, message);

        System.out.println("Gesendet: " + message.returnId()
                + " -> " + message.newStatus()
                + " (Routing-Key: " + routingKey + ", Quelle: " + message.source() + ")");
    }
}
