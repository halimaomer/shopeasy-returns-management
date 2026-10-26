
package de.shopeasy.returnsmanagement.model;

import java.time.Instant;
import java.util.List;

public record ReturnRequest(
    String returnId,
    String orderId,
    String marketplace,
    String status,
    String reason,
    String reasonText,
    Instant requestedAt,
    Instant updatedAt,
    List<ReturnItem> items,
    Customer customer,
    String returnLabelUrl
) {
}
