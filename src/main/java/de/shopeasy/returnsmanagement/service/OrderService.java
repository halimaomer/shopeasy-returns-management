
package de.shopeasy.returnsmanagement.service;

import de.shopeasy.returnsmanagement.model.Order;
import de.shopeasy.returnsmanagement.repository.OrderRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Die Bestellung mit der ID " + orderId
                                + " wurde nicht gefunden."
                ));
    }
}
