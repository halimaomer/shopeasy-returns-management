
package de.shopeasy.returnsmanagement.service;

import de.shopeasy.returnsmanagement.model.ReturnRequest;
import de.shopeasy.returnsmanagement.repository.ReturnRepository;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReturnService {

    private final ReturnRepository returnRepository;

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

        return returnRepository.findAll()
                .stream()
                .filter(ret -> status == null || status.isBlank()
                        || ret.status().equalsIgnoreCase(status))
                .filter(ret -> marketplace == null || marketplace.isBlank()
                        || ret.marketplace().equalsIgnoreCase(marketplace))
                .filter(ret -> reason == null || reason.isBlank()
                        || ret.reason().equalsIgnoreCase(reason))
                .toList();
    }
}
