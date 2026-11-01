package de.shopeasy.returnsmanagement.repository;

import de.shopeasy.returnsmanagement.model.Order;
import de.shopeasy.returnsmanagement.model.ReturnItem;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class OrderRepository {

    // Speicher: orderId -> Bestellung
    private final Map<String, Order> orders = new LinkedHashMap<>();

    public OrderRepository() {
        add("ORD-2026-118932", "ZALANDO", "CUST-88213", "2026-09-10T12:30:00Z",
                List.of(item("SH-BLZ-042-M", "Blazer Slim Fit", 1, "89.90")));

        add("ORD-2026-118940", "AMAZON", "CUST-90117", "2026-09-12T14:15:00Z",
                List.of(item("SH-TSH-101-L", "Basic T-Shirt", 2, "19.90")));

        add("ORD-2026-118955", "ZALANDO", "CUST-77452", "2026-09-13T10:20:00Z",
                List.of(item("SH-JNS-220-32", "Slim Jeans", 1, "69.90")));

        add("ORD-2026-118961", "AMAZON", "CUST-81236", "2026-09-14T17:45:00Z",
                List.of(item("SH-JKT-310-M", "Übergangsjacke", 1, "129.00")));

        add("ORD-2026-118977", "ZALANDO", "CUST-66019", "2026-09-15T09:10:00Z",
                List.of(item("SH-KLD-415-S", "Sommerkleid", 1, "59.90"),
                        item("SH-GRT-020-U", "Stoffgürtel", 1, "14.90")));

        add("ORD-2026-118983", "AMAZON", "CUST-90544", "2026-09-16T19:30:00Z",
                List.of(item("SH-SNK-505-42", "Sneaker Low", 1, "99.90")));

        add("ORD-2026-118990", "ZALANDO", "CUST-55871", "2026-09-17T08:05:00Z",
                List.of(item("SH-ABK-610-38", "Abendkleid", 1, "179.00")));

        add("ORD-2026-119002", "AMAZON", "CUST-71203", "2026-09-18T13:40:00Z",
                List.of(item("SH-HMD-120-M", "Business Hemd", 1, "49.90")));

        add("ORD-2026-119010", "ZALANDO", "CUST-62948", "2026-09-19T15:25:00Z",
                List.of(item("SH-PLV-330-L", "Strickpullover", 1, "74.90")));

        add("ORD-2026-119024", "AMAZON", "CUST-88213", "2026-10-20T11:50:00Z",
                List.of(item("SH-HDY-205-XL", "Hoodie Oversize", 1, "54.90")));
    }

    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    // Hilfsmethode zum Anlegen einer Bestellung
    private void add(String orderId, String marketplace, String customerId,
                     String orderedAt, List<ReturnItem> items) {
        orders.put(orderId, new Order(orderId, marketplace, customerId,
                Instant.parse(orderedAt), items));
    }

    // Hilfsmethode für eine Bestellposition (immer EUR)
    private static ReturnItem item(String sku, String productName, int quantity, String price) {
        return new ReturnItem(sku, productName, quantity, new BigDecimal(price), "EUR");
    }
}
