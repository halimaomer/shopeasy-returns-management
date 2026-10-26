package de.shopeasy.returnsmanagement.repository;

import de.shopeasy.returnsmanagement.model.Customer;
import de.shopeasy.returnsmanagement.model.ReturnItem;
import de.shopeasy.returnsmanagement.model.ReturnRequest;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ReturnRepository {

    // Speicher: returnId -> Retoure
    private final Map<String, ReturnRequest> returns = new LinkedHashMap<>();

    public ReturnRepository() {
        add("RET-2026-00457", "ORD-2026-118932", "ZALANDO", "NEW", "SIZE_TOO_SMALL",
                "Passt leider nicht, eine Nummer kleiner bitte.", "2026-09-18T09:14:00Z",
                List.of(item("SH-BLZ-042-M", "Blazer Slim Fit", 1, "89.90")),
                new Customer("CUST-88213", "j.mueller@example.com"));

        add("RET-2026-00458", "ORD-2026-118940", "AMAZON", "NEW", "WRONG_ITEM",
                "Falsche Farbe geliefert, bestellt war dunkelblau.", "2026-09-18T11:02:00Z",
                List.of(item("SH-TSH-101-L", "Basic T-Shirt", 2, "19.90")),
                new Customer("CUST-90117", "a.schmidt@example.com"));

        add("RET-2026-00459", "ORD-2026-118955", "ZALANDO", "APPROVED", "SIZE_TOO_LARGE",
                "Fällt zu groß aus.", "2026-09-19T08:45:00Z",
                List.of(item("SH-JNS-220-32", "Slim Jeans", 1, "69.90")),
                new Customer("CUST-77452", "m.yilmaz@example.com"));

        add("RET-2026-00460", "ORD-2026-118961", "AMAZON", "LABEL_CREATED", "DEFECTIVE",
                "Reißverschluss klemmt.", "2026-09-19T14:20:00Z",
                List.of(item("SH-JKT-310-M", "Übergangsjacke", 1, "129.00")),
                new Customer("CUST-81236", "l.weber@example.com"));

        add("RET-2026-00461", "ORD-2026-118977", "ZALANDO", "RECEIVED", "NOT_AS_DESCRIBED",
                "Material fühlt sich anders an als beschrieben.", "2026-09-20T10:05:00Z",
                List.of(item("SH-KLD-415-S", "Sommerkleid", 1, "59.90"),
                        item("SH-GRT-020-U", "Stoffgürtel", 1, "14.90")),
                new Customer("CUST-66019", "s.becker@example.com"));

        add("RET-2026-00462", "ORD-2026-118983", "AMAZON", "INSPECTED", "CHANGED_MIND",
                "Gefällt mir doch nicht.", "2026-09-21T16:40:00Z",
                List.of(item("SH-SNK-505-42", "Sneaker Low", 1, "99.90")),
                new Customer("CUST-90544", "t.hoffmann@example.com"));

        add("RET-2026-00463", "ORD-2026-118990", "ZALANDO", "REJECTED", "CHANGED_MIND",
                "Nach dem Event nicht mehr benötigt.", "2026-09-22T09:30:00Z",
                List.of(item("SH-ABK-610-38", "Abendkleid", 1, "179.00")),
                new Customer("CUST-55871", "n.krueger@example.com"));

        add("RET-2026-00464", "ORD-2026-119002", "AMAZON", "REFUNDED", "SIZE_TOO_SMALL",
                "Zu eng an den Schultern.", "2026-09-23T12:15:00Z",
                List.of(item("SH-HMD-120-M", "Business Hemd", 1, "49.90")),
                new Customer("CUST-71203", "p.wagner@example.com"));

        add("RET-2026-00465", "ORD-2026-119010", "ZALANDO", "CANCELLED", "WRONG_ITEM",
                "Anderen Artikel erhalten.", "2026-09-24T07:55:00Z",
                List.of(item("SH-PLV-330-L", "Strickpullover", 1, "74.90")),
                new Customer("CUST-62948", "k.schulz@example.com"));

        add("RET-2026-00466", "ORD-2026-119024", "AMAZON", "NEW", "DEFECTIVE",
                "Naht am Ärmel ist aufgegangen.", "2026-10-25T18:10:00Z",
                List.of(item("SH-HDY-205-XL", "Hoodie Oversize", 1, "54.90")),
                new Customer("CUST-88213", "j.mueller@example.com"));
    }



    public List<ReturnRequest> findAll() {
        return new ArrayList<>(returns.values());
    }

    public Optional<ReturnRequest> findById(String returnId) {
        return Optional.ofNullable(returns.get(returnId));
    }

    public ReturnRequest save(ReturnRequest returnRequest) {
        returns.put(returnRequest.returnId(), returnRequest);
        return returnRequest;
    }


    private void add(String returnId, String orderId, String marketplace, String status,
                     String reason, String reasonText, String requestedAt,
                     List<ReturnItem> items, Customer customer) {
        Instant time = Instant.parse(requestedAt);
        String labelUrl = (status.equals("NEW") || status.equals("REJECTED"))
                ? null
                : "https://labels.example.com/" + returnId + ".pdf";

        returns.put(returnId, new ReturnRequest(returnId, orderId, marketplace, status,
                reason, reasonText, time, time, items, customer, labelUrl));
    }

    private static ReturnItem item(String sku, String productName, int quantity, String price) {
        return new ReturnItem(sku, productName, quantity, new BigDecimal(price), "EUR");
    }
}
