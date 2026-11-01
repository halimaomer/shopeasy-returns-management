package de.shopeasy.returnsmanagement.messaging;

import de.shopeasy.returnsmanagement.model.ReturnRequest;
import de.shopeasy.returnsmanagement.service.ReturnService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

// Liest Statusänderungen aus der Queue "status-updater" und aktualisiert
// den Status der Retoure. Es gelten dieselben Statusregeln wie bei der REST-API.
@Component
public class StatusUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(StatusUpdateConsumer.class);

    private final ReturnService returnService;

    public StatusUpdateConsumer(ReturnService returnService) {
        this.returnService = returnService;
    }

    // Wird automatisch aufgerufen, sobald eine Nachricht in der Queue ankommt
    @RabbitListener(queues = RabbitMqConfig.STATUS_UPDATER_QUEUE)
    public void onStatusUpdate(StatusUpdateMessage message) {

        log.info("Statusänderung empfangen: {} -> {} (Quelle: {}, Nachricht: {})",
                message.returnId(), message.newStatus(),
                message.source(), message.messageId());

        try {
            ReturnRequest updated = returnService.updateReturnStatus(
                    message.returnId(),
                    message.newStatus());

            log.info("Status aktualisiert: {} ist jetzt {}",
                    updated.returnId(), updated.status());

        } catch (ResponseStatusException exception) {
            // Ungültiger Übergang (409), unbekannte Retoure (404) oder ungültiger Status (400):
            // Nachricht ablehnen, ohne sie erneut zuzustellen -> Dead-Letter-Queue
            log.warn("Statusänderung abgelehnt: {} -> {}: {}",
                    message.returnId(), message.newStatus(), exception.getReason());

            throw new AmqpRejectAndDontRequeueException(
                    "Statusänderung nicht möglich: " + exception.getReason(), exception);
        }
    }
}
