package de.shopeasy.returnsmanagement.repository;

import de.shopeasy.returnsmanagement.model.Order;
import de.shopeasy.returnsmanagement.model.ReturnItem;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;

@Repository
public class OrderRepository {

    private final Map<String, Order> orders = new LinkedHashMap<>();

    public OrderRepository() {

        Order order1 = new Order(
                "ORD-2026-118932",
                "ZALANDO",
                "CUST-88213",
                Instant.parse("2026-09-10T12:30:00Z"),
                List.of(new ReturnItem(
                        "SH-BLZ-042-M",
                        "Blazer Slim Fit",
                        1,
                        new BigDecimal("89.90"),
                        "EUR"
                ))
        );

        Order order2 = new Order(
                "ORD-2026-118940",
                "AMAZON",
                "CUST-90117",
                Instant.parse("2026-09-12T14:15:00Z"),
                List.of(new ReturnItem(
                        "SH-TSH-101-L",
                        "Basic T-Shirt",
                        2,
                        new BigDecimal("19.90"),
                        "EUR"
                ))
        );

        orders.put(order1.orderId(), order1);
        orders.put(order2.orderId(), order2);
    }

    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }
}
