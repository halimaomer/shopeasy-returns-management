
package de.shopeasy.returnsmanagement.client;

import de.shopeasy.returnsmanagement.model.Order;
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

    // Testen, ob alle zehn Retouren abgerufen werden
    @Test
    void testGetAllReturns() {

        List<ReturnRequest> returns =
                returnApiClient.getAllReturns();

        assertEquals(10, returns.size());
    }

    // Testen, ob eine bestimmte Retoure gefunden wird
    @Test
    void testGetReturnById() {

        ReturnRequest result =
                returnApiClient.getReturnById("RET-2026-00457");

        assertNotNull(result);
        assertEquals("RET-2026-00457", result.returnId());
        assertEquals("ZALANDO", result.marketplace());
    }

    // Testen, ob eine Bestellung anhand ihrer ID abgerufen wird
    @Test
    void testGetOrderById() {

        Order result =
                returnApiClient.getOrderById("ORD-2026-118932");

        assertNotNull(result);
        assertEquals("ORD-2026-118932", result.orderId());
        assertEquals("ZALANDO", result.marketplace());
        assertNotNull(result.items());
        assertFalse(result.items().isEmpty());
    }

    // Testen, ob Retouren nach Status und Marktplatz gefiltert werden
    @Test
    void testFilterReturns() {

        List<ReturnRequest> returns =
                returnApiClient.filterReturns(
                        "NEW",
                        "AMAZON",
                        null
                );

        assertEquals(2, returns.size());
    }

    // Testen, ob der Status über eine JSON-PATCH-Anfrage geändert wird
    @Test
    void testUpdateReturnStatus() {

        ReturnRequest result =
                returnApiClient.updateReturnStatus(
                        "RET-2026-00457",
                        "APPROVED"
                );

        assertNotNull(result);
        assertEquals("RET-2026-00457", result.returnId());
        assertEquals("APPROVED", result.status());

        // Prüfen, ob der neue Status auch gespeichert wurde
        ReturnRequest savedReturn =
                returnApiClient.getReturnById("RET-2026-00457");

        assertEquals("APPROVED", savedReturn.status());
    }
}
