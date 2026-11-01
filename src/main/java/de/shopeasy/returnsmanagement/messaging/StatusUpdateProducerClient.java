package de.shopeasy.returnsmanagement.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

// Eigenständiger Producer-Client. Simuliert andere Systeme (Marktplatz, Lager),
// die Statusänderungen an RabbitMQ melden.
//
// Start ohne Argumente: verschickt mehrere Beispiel-Szenarien
// Start mit Argumenten: <returnId> <neuerStatus> [quelle]
//   z. B. RET-2026-00466 APPROVED MARKETPLACE
public class StatusUpdateProducerClient {

    public static void main(String[] args) {

        // Verbindung zu RabbitMQ aufbauen (Zugangsdaten wie in docker-compose.yml)
        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory("localhost", 5672);
        connectionFactory.setUsername("shopeasy");
        connectionFactory.setPassword("shopeasy");

        try {
            // Exchange anlegen, falls sie noch nicht existiert.
            // Der Producer legt nur die Exchange an – die Queues gehören zum Consumer.
            RabbitAdmin admin = new RabbitAdmin(connectionFactory);
            admin.declareExchange(new TopicExchange(RabbitMqConfig.STATUS_EXCHANGE));

            // RabbitTemplate mit JSON-Converter zum Senden
            RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
            rabbitTemplate.setMessageConverter(new JacksonJsonMessageConverter());

            StatusUpdateProducer producer = new StatusUpdateProducer(rabbitTemplate);

            if (args.length >= 2) {
                // Einzelne Nachricht aus den Programmargumenten
                String source = args.length >= 3 ? args[2] : "MANUAL";
                producer.send(StatusUpdateMessage.of(args[0], args[1], source));

            } else {
                sendDemoScenarios(producer);
            }

        } finally {
            // Verbindung schließen
            connectionFactory.destroy();
        }
    }

    // Verschickt typische Szenarien aus dem Retourenprozess
    private static void sendDemoScenarios(StatusUpdateProducer producer) {

        System.out.println("--- Gültige Statusänderungen ---");

        // RET-2026-00466 ist NEW: Marktplatz genehmigt, danach wird das Label erstellt
        producer.send(StatusUpdateMessage.of("RET-2026-00466", "APPROVED", "MARKETPLACE"));
        producer.send(StatusUpdateMessage.of("RET-2026-00466", "LABEL_CREATED", "MARKETPLACE"));

        // RET-2026-00460 ist LABEL_CREATED: Lager meldet den Wareneingang
        producer.send(StatusUpdateMessage.of("RET-2026-00460", "RECEIVED", "WAREHOUSE"));

        System.out.println("--- Fehlerfälle (landen in der Dead-Letter-Queue) ---");

        // Ungültiger Übergang: RET-2026-00464 ist bereits REFUNDED (Endstatus)
        producer.send(StatusUpdateMessage.of("RET-2026-00464", "APPROVED", "MARKETPLACE"));

        // Unbekannte Retoure
        producer.send(StatusUpdateMessage.of("RET-9999-99999", "APPROVED", "MARKETPLACE"));

        // Unbekannter Status
        producer.send(StatusUpdateMessage.of("RET-2026-00458", "LOST", "WAREHOUSE"));
    }
}
