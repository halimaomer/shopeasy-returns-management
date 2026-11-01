package de.shopeasy.returnsmanagement.service;

import de.shopeasy.returnsmanagement.model.ReturnRequest;
import de.shopeasy.returnsmanagement.repository.ReturnRepository;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class ReturnService {

    // Repository für den Zugriff auf die gespeicherten Retouren
    private final ReturnRepository returnRepository;

    // Konstruktor für die automatische Einbindung des Repositorys
    public ReturnService(ReturnRepository returnRepository) {
        this.returnRepository = returnRepository;
    }

    // Alle vorhandenen Retouren abrufen
    public List<ReturnRequest> getAllReturns() {
        return returnRepository.findAll();
    }

    // Retouren optional nach Status, Marktplatz und Grund filtern
    // Nicht angegebene Filter werden ignoriert
    public List<ReturnRequest> filterReturns(
            String status,
            String marketplace,
            String reason) {

        return returnRepository.findAll()
                .stream()
                .filter(ret -> status == null
                        || status.isBlank()
                        || ret.status().equalsIgnoreCase(status))
                .filter(ret -> marketplace == null
                        || marketplace.isBlank()
                        || ret.marketplace().equalsIgnoreCase(marketplace))
                .filter(ret -> reason == null
                        || reason.isBlank()
                        || ret.reason().equalsIgnoreCase(reason))
                .toList();
    }

    // Einzelne Retoure anhand ihrer ID abrufen
    // Falls die Retoure nicht existiert, wird HTTP 404 ausgelöst
    public ReturnRequest getReturnById(String returnId) {

        return returnRepository.findById(returnId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Die Retoure mit der ID " + returnId
                                + " existiert nicht."
                ));
    }

    // Status einer bestehenden Retoure aktualisieren
    public ReturnRequest updateReturnStatus(
            String returnId,
            String newStatus) {

        // Prüfen, ob die Retoure existiert
        ReturnRequest existingReturn = getReturnById(returnId);

        // Prüfen, ob ein neuer Status angegeben wurde
        if (newStatus == null || newStatus.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Der neue Status darf nicht leer sein."
            );
        }

        // Status vereinheitlichen, beispielsweise approved zu APPROVED
        String status = newStatus.trim().toUpperCase();

        // Erlaubte Statuswerte definieren
        List<String> allowedStatuses = List.of(
                "NEW",
                "APPROVED",
                "LABEL_CREATED",
                "RECEIVED",
                "INSPECTED",
                "REJECTED",
                "REFUNDED",
                "CANCELLED"
        );

        // Ungültige Statuswerte ablehnen
        if (!allowedStatuses.contains(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ungültiger Status: " + status
            );
        }

        // Endstatuswerte definieren
        // Abgeschlossene Retouren dürfen nicht erneut geändert werden
        List<String> finalStatuses = List.of(
                "REFUNDED",
                "REJECTED",
                "CANCELLED"
        );

        // Prüfen, ob die Retoure bereits abgeschlossen ist
        if (finalStatuses.contains(existingReturn.status())
                && !existingReturn.status().equals(status)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Die Retoure hat bereits den Endstatus "
                            + existingReturn.status()
                            + " und kann nicht mehr geändert werden."
            );
        }

        // Retoure mit aktualisiertem Status erstellen
        // Die übrigen Daten bleiben unverändert
        ReturnRequest updatedReturn = new ReturnRequest(
                existingReturn.returnId(),
                existingReturn.orderId(),
                existingReturn.marketplace(),
                status,
                existingReturn.reason(),
                existingReturn.reasonText(),
                existingReturn.requestedAt(),
                Instant.now(),
                existingReturn.items(),
                existingReturn.customer(),
                existingReturn.returnLabelUrl()
        );

        // Aktualisierte Retoure im Repository speichern
        return returnRepository.save(updatedReturn);
    }
}