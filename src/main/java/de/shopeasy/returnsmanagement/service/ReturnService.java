package de.shopeasy.returnsmanagement.service;

import de.shopeasy.returnsmanagement.model.ReturnRequest;
import de.shopeasy.returnsmanagement.model.ReturnStatus;
import de.shopeasy.returnsmanagement.repository.ReturnRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class ReturnService {

    private final ReturnRepository returnRepository;

    // Repository über den Konstruktor einbinden
    public ReturnService(ReturnRepository returnRepository) {
        this.returnRepository = returnRepository;
    }

    // Alle Retouren abrufen
    public List<ReturnRequest> getAllReturns() {
        return returnRepository.findAll();
    }

    // Retouren nach Status, Marktplatz und Grund filtern
    public List<ReturnRequest> filterReturns(
            String status,
            String marketplace,
            String reason) {

        return returnRepository.findAll().stream()
                .filter(r -> status == null || status.isBlank()
                        || r.status().equalsIgnoreCase(status))
                .filter(r -> marketplace == null || marketplace.isBlank()
                        || r.marketplace().equalsIgnoreCase(marketplace))
                .filter(r -> reason == null || reason.isBlank()
                        || r.reason().equalsIgnoreCase(reason))
                .toList();
    }

    // Einzelne Retoure anhand der ID suchen
    public ReturnRequest getReturnById(String returnId) {
        return returnRepository.findById(returnId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Retoure nicht gefunden: " + returnId));
    }

    // Status einer Retoure aktualisieren
    public ReturnRequest updateReturnStatus(String returnId, String newStatus) {

        // Prüfen, ob ein neuer Status angegeben wurde
        if (newStatus == null || newStatus.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ein neuer Status muss angegeben werden");
        }

        // Retoure laden
        ReturnRequest existingReturn = getReturnById(returnId);

        ReturnStatus currentStatus;
        ReturnStatus targetStatus;

        try {
            // Statuswerte in Enum-Werte umwandeln
            currentStatus = ReturnStatus.valueOf(
                    existingReturn.status().trim().toUpperCase(Locale.ROOT));

            targetStatus = ReturnStatus.valueOf(
                    newStatus.trim().toUpperCase(Locale.ROOT));

        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ungültiger Retourenstatus");
        }

        // Prüfen, ob der gewünschte Statusübergang erlaubt ist
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Statuswechsel von " + currentStatus
                            + " zu " + targetStatus + " ist nicht erlaubt");
        }

        // Neue Retoure mit aktualisiertem Status erstellen
        ReturnRequest updatedReturn = new ReturnRequest(
                existingReturn.returnId(),
                existingReturn.orderId(),
                existingReturn.marketplace(),
                targetStatus.name(),
                existingReturn.reason(),
                existingReturn.reasonText(),
                existingReturn.requestedAt(),
                Instant.now(),
                existingReturn.items(),
                existingReturn.customer(),
                existingReturn.returnLabelUrl()
        );

        // Aktualisierte Retoure speichern
        returnRepository.save(updatedReturn);

        return updatedReturn;
    }
}
