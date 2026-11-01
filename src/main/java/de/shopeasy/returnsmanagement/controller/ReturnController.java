
package de.shopeasy.returnsmanagement.controller;

import de.shopeasy.returnsmanagement.model.ReturnRequest;
import de.shopeasy.returnsmanagement.model.ReturnListResponse;
import de.shopeasy.returnsmanagement.service.ReturnService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/returns")
public class ReturnController {

    private final ReturnService returnService;

    // Service über den Konstruktor einbinden
    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    // Alle Retouren abrufen und optional filtern
    // Die Liste wird innerhalb des JSON-Feldes "data" zurückgegeben
    @GetMapping
    public ReturnListResponse getAllReturns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String marketplace,
            @RequestParam(required = false) String reason) {

        return new ReturnListResponse(
                returnService.filterReturns(
                        status,
                        marketplace,
                        reason
                )
        );
    }

    // Einzelne Retoure anhand ihrer ID abrufen
    @GetMapping("/{returnId}")
    public ReturnRequest getReturnById(
            @PathVariable String returnId) {

        return returnService.getReturnById(returnId);
    }

    // Status einer Retoure über eine JSON-Anfrage ändern
    @PatchMapping(
            value = "/{returnId}/status",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ReturnRequest updateReturnStatus(
            @PathVariable String returnId,
            @RequestBody StatusUpdateRequest request) {

        return returnService.updateReturnStatus(
                returnId,
                request.status()
        );
    }

    // Beschreibt das erwartete JSON-Format für Statusänderungen
    public record StatusUpdateRequest(String status) {
    }
}
