package de.shopeasy.returnsmanagement.model;

import java.time.Instant;
import java.util.List;

public record Order(
        String orderId,
        String marketplace,
        String customerId,
        Instant orderedAt,
        List<ReturnItem> items
) {}
