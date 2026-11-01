package de.shopeasy.returnsmanagement.messaging;

import java.time.Instant;
import java.util.UUID;

// Nachricht über eine Statusänderung einer Retoure.
// Wird vom Producer als JSON an RabbitMQ geschickt und vom Consumer gelesen.
public record StatusUpdateMessage(
        String messageId,   // eindeutige ID der Nachricht (z. B. zur Nachverfolgung)
        String returnId,    // betroffene Retoure, z. B. "RET-2026-00457"
        String newStatus,   // gewünschter neuer Status, z. B. "APPROVED"
        String source,      // meldendes System, z. B. "WAREHOUSE" oder "MARKETPLACE"
        Instant occurredAt  // Zeitpunkt der Statusänderung
) {

    // Erstellt eine neue Nachricht mit automatisch erzeugter ID und aktuellem Zeitpunkt
    public static StatusUpdateMessage of(String returnId, String newStatus, String source) {
        return new StatusUpdateMessage(
                UUID.randomUUID().toString(),
                returnId,
                newStatus,
                source,
                Instant.now()
        );
    }
}
