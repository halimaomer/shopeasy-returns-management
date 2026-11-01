package de.shopeasy.returnsmanagement.client;

import de.shopeasy.returnsmanagement.model.ReturnRequest;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

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

    // Alle Retouren über die REST-API abrufen
    public List<ReturnRequest> getAllReturns() {

        ReturnRequest[] returns = restClient.get()
                .uri("/returns")
                .retrieve()
                .body(ReturnRequest[].class);

        // Antwort in eine Liste umwandeln
        return returns == null
                ? List.of()
                : Arrays.asList(returns);
    }

    // Einzelne Retoure anhand ihrer ID abrufen
    public ReturnRequest getReturnById(String returnId) {

        return restClient.get()
                .uri("/returns/{returnId}", returnId)
                .retrieve()
                .body(ReturnRequest.class);
    }

    // Retouren nach Status, Marktplatz und Grund filtern
    public List<ReturnRequest> filterReturns(
            String status,
            String marketplace,
            String reason) {

        ReturnRequest[] returns = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/returns")
                        .queryParamIfPresent(
                                "status",
                                java.util.Optional.ofNullable(status)
                                        .filter(s -> !s.isBlank()))
                        .queryParamIfPresent(
                                "marketplace",
                                java.util.Optional.ofNullable(marketplace)
                                        .filter(s -> !s.isBlank()))
                        .queryParamIfPresent(
                                "reason",
                                java.util.Optional.ofNullable(reason)
                                        .filter(s -> !s.isBlank()))
                        .build())
                .retrieve()
                .body(ReturnRequest[].class);

        return returns == null
                ? List.of()
                : Arrays.asList(returns);
    }

    // Status einer Retoure über PATCH aktualisieren
    public ReturnRequest updateReturnStatus(
            String returnId,
            String newStatus) {

        return restClient.patch()
                .uri("/returns/{returnId}/status", returnId)
                .contentType(
                        org.springframework.http.MediaType.TEXT_PLAIN)
                .body(newStatus)
                .retrieve()
                .body(ReturnRequest.class);
    }
}