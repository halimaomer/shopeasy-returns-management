
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

    public List<ReturnRequest> getAllReturns() {
        return returnRepository.findAll();
    }

    
}
