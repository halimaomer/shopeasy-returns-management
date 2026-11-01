package de.shopeasy.returnsmanagement.client;

import de.shopeasy.returnsmanagement.model.ReturnRequest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ReturnApiClientTest {

    // REST-API-Client automatisch einbinden
    @Autowired
    private ReturnApiClient returnApiClient;

    // Testen, ob alle Retouren abgerufen werden
    @Test
    void testGetAllReturns() {

        List<ReturnRequest> returns = returnApiClient.getAllReturns();

        assertEquals(10, returns.size());
    }

    // Testen, ob eine bestimmte Retoure gefunden wird
    @Test
    void testGetReturnById() {

        ReturnRequest result =
                returnApiClient.getReturnById("RET-2026-00457");

        assertEquals("RET-2026-00457", result.returnId());
        assertEquals("ZALANDO", result.marketplace());
    }

    // Testen, ob die Filterung funktioniert
    @Test
    void testFilterReturns() {

        List<ReturnRequest> returns =
                returnApiClient.filterReturns("NEW", "AMAZON", null);

        assertEquals(2, returns.size());
    }

    // Testen, ob der Status über PATCH geändert wird
    @Test
    void testUpdateReturnStatus() {

        ReturnRequest result =
                returnApiClient.updateReturnStatus(
                        "RET-2026-00457",
                        "APPROVED"
                );

        assertEquals("APPROVED", result.status());
    }
}
