
package de.shopeasy.returnsmanagement.model;

import java.util.List;

// Einheitliches Antwortformat für mehrere Retouren
public record ReturnListResponse(
        List<ReturnRequest> data
) {
}
