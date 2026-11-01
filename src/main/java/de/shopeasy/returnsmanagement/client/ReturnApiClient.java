
package de.shopeasy.returnsmanagement.client;

import de.shopeasy.returnsmanagement.model.Order;
import de.shopeasy.returnsmanagement.model.ReturnListResponse;
import de.shopeasy.returnsmanagement.model.ReturnRequest;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ReturnApiClient {

    // REST-Client für HTTP-Anfragen
    private final RestClient restClient;

    // REST-Client für die lokale ShopEasy-API erstellen
    public ReturnApiClient() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8080")
                .build();
    }

    // Alle Retouren aus dem JSON-Feld "data" abrufen
    public List<ReturnRequest> getAllReturns() {

        ReturnListResponse response = restClient.get()
                .uri("/returns")
                .retrieve()
                .body(ReturnListResponse.class);

        return extractReturns(response);
    }

    // Einzelne Retoure anhand ihrer ID abrufen
    public ReturnRequest getReturnById(String returnId) {

        return restClient.get()
                .uri("/returns/{returnId}", returnId)
                .retrieve()
                .body(ReturnRequest.class);
    }

    // Bestellung anhand ihrer Bestellnummer abrufen
    public Order getOrderById(String orderId) {

        return restClient.get()
                .uri("/orders/{orderId}", orderId)
                .retrieve()
                .body(Order.class);
    }

    // Retouren nach Status, Marktplatz und Grund filtern
    public List<ReturnRequest> filterReturns(
            String status,
            String marketplace,
            String reason) {

        ReturnListResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/returns")
                        .queryParamIfPresent(
                                "status",
                                Optional.ofNullable(status)
                                        .filter(s -> !s.isBlank()))
                        .queryParamIfPresent(
                                "marketplace",
                                Optional.ofNullable(marketplace)
                                        .filter(s -> !s.isBlank()))
                        .queryParamIfPresent(
                                "reason",
                                Optional.ofNullable(reason)
                                        .filter(s -> !s.isBlank()))
                        .build())
                .retrieve()
                .body(ReturnListResponse.class);

        return extractReturns(response);
    }

    // Status einer Retoure über eine JSON-Anfrage aktualisieren
    public ReturnRequest updateReturnStatus(
            String returnId,
            String newStatus) {

        Map<String, String> requestBody = Map.of(
                "status",
                newStatus
        );

        return restClient.patch()
                .uri("/returns/{returnId}/status", returnId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(ReturnRequest.class);
    }

    // Retourenliste aus der API-Antwort auslesen
    private List<ReturnRequest> extractReturns(
            ReturnListResponse response) {

        if (response == null || response.data() == null) {
            return List.of();
        }

        return response.data();
    }
}
