package com.mobylab.springbackend.controller;

import com.mobylab.springbackend.exception.BadRequestException;
import com.mobylab.springbackend.service.OrderService;
import com.mobylab.springbackend.service.dto.OrderDto;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController implements SecuredRestController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderDto> placeOrder(@RequestBody OrderDto orderDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();

        orderDto.setClientEmail(currentUserEmail);
        logger.info("User '{}' requesting to place order", currentUserEmail);


        OrderDto createdOrder = orderService.placeOrder(orderDto);
        logger.info("Successfully placed order with ID {} for user '{}'", createdOrder.getId(), currentUserEmail);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);

    }

    /**
     * Gets a specific order by its ID.
     * Security check: Allows ADMIN or the user who placed the order.
     * Uses the secure service method.
     *
     * @param id The UUID of the order.
     * @return The OrderDto if found and authorized, otherwise 404 or 403.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("User '{}' requesting order with ID {}", authentication.getName(), id);


        OrderDto order = orderService.getOrderByIdAndValidateUser(id, authentication.getName(), authentication.getAuthorities());
        logger.info("Returning order ID {}", id);
        return ResponseEntity.ok(order);

    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<OrderDto>> getAllOrders() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Admin user '{}' requesting all orders", authentication.getName());
        List<OrderDto> orders = orderService.getAllOrders();
        logger.info("Returning {} orders", orders.size());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderDto>> getMyOrders() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        logger.info("User '{}' requesting their orders", currentUserEmail);

        List<OrderDto> orders = orderService.getOrdersByUserEmail(currentUserEmail);
        logger.info("Returning {} orders for user '{}'", orders.size(), currentUserEmail);
        return ResponseEntity.ok(orders);

    }

    /**
     * Updates the status of an order. ADMIN only.
     * @param id The order ID.
     * @param status The new status string (consider using an Enum or specific DTO).
     * @return The updated OrderDto.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<OrderDto> updateOrderStatus(@PathVariable UUID id, @RequestBody String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new BadRequestException("New status cannot be empty.");
        }
        String cleanStatus = status.replace("\"", "");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Admin user '{}' updating status for order {} to '{}'", authentication.getName(), id, cleanStatus);

        OrderDto updatedOrder = orderService.updateOrderStatus(id, cleanStatus);
        logger.info("Successfully updated status for order {}", id);
        return ResponseEntity.ok(updatedOrder);
    }
}