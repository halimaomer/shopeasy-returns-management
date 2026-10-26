
package de.shopeasy.returnsmanagement.model;

import java.math.BigDecimal;

public record ReturnItem(
    String sku,
    String productName,
    int quantity,
    BigDecimal unitPrice,
    String currency
) {
}
