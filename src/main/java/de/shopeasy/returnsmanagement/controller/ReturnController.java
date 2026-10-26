
package de.shopeasy.returnsmanagement.controller;

import de.shopeasy.returnsmanagement.model.ReturnRequest;
import de.shopeasy.returnsmanagement.service.ReturnService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/returns")
public class ReturnController {

    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @GetMapping
    public List<ReturnRequest> getAllReturns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String marketplace,
            @RequestParam(required = false) String reason) {

        return returnService.filterReturns(
                status,
                marketplace,
                reason
        );
    }
}
