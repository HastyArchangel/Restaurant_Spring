package com.mobylab.springbackend.service;

import com.mobylab.springbackend.entity.Dish;
import com.mobylab.springbackend.entity.Order;
import com.mobylab.springbackend.entity.User;
import com.mobylab.springbackend.exception.BadRequestException;
import com.mobylab.springbackend.repository.DishRepository;
import com.mobylab.springbackend.repository.OrderRepository;
import com.mobylab.springbackend.repository.UserRepository;
import com.mobylab.springbackend.service.dto.OrderDto;
// import com.mobylab.springbackend.service.mapper.OrderMapper; // REMOVED Mapper import
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList; // Added for manual list creation
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DishRepository dishRepository;
    private final EmailNotificationService emailNotificationService;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        DishRepository dishRepository,
                        EmailNotificationService emailNotificationService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.dishRepository = dishRepository;
        this.emailNotificationService = emailNotificationService;
    }

    // --- Manual Mapping Helper Methods ---
    private OrderDto mapOrderToDto(Order order) {
        if (order == null) {
            return null;
        }
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus());

        if (order.getClient() != null) {
            dto.setClientEmail(order.getClient().getEmail());
        }

        if (order.getDishes() != null) {
            List<UUID> dishIds = order.getDishes().stream()
                    .map(Dish::getId)
                    .collect(Collectors.toList());
            dto.setDishIds(dishIds);
        } else {
            dto.setDishIds(new ArrayList<>());
        }

        return dto;
    }

    private List<OrderDto> mapOrderListToDtoList(List<Order> orders) {
        if (orders == null) {
            return new ArrayList<>();
        }
        return orders.stream()
                .map(this::mapOrderToDto)
                .collect(Collectors.toList());
    }
    // --- End Manual Mapping ---


    public OrderDto placeOrder(OrderDto orderDto) {
        logger.debug("Attempting to place order for client email: {}", orderDto.getClientEmail());

        User client = userRepository.findUserByEmail(orderDto.getClientEmail())
                .orElseThrow(() -> {
                    logger.error("User not found for email: {}", orderDto.getClientEmail());
                    return new EntityNotFoundException("Client user not found with email: " + orderDto.getClientEmail());
                });

        if (orderDto.getDishIds() == null || orderDto.getDishIds().isEmpty()) {
            throw new BadRequestException("Order must contain at least one dish.");
        }
        List<Dish> dishes = dishRepository.findAllById(orderDto.getDishIds());
        if (dishes.size() != orderDto.getDishIds().size()) {
            List<UUID> foundIds = dishes.stream().map(Dish::getId).collect(Collectors.toList());
            List<UUID> missingIds = orderDto.getDishIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            logger.error("Some dishes not found. Requested: {}, Found: {}, Missing: {}", orderDto.getDishIds(), foundIds, missingIds);
            throw new EntityNotFoundException("Could not find all dishes. Missing IDs: " + missingIds);
        }

        Order order = new Order();
        order.setClient(client);
        order.setDishes(dishes);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PLACED");

        Order savedOrder = orderRepository.save(order);
        logger.info("Order {} placed successfully for user {}", savedOrder.getId(), client.getEmail());

        try {
            String subject = "Your Order Confirmation (ID: " + savedOrder.getId() + ")";
            String body = "Dear " + client.getUsername() + ",\n\nYour order has been placed successfully and is now being processed.\n\nOrder ID: " + savedOrder.getId() + "\nStatus: " + savedOrder.getStatus() + "\n\nThank you for your order!";
            emailNotificationService.sendOrderConfirmation(client.getEmail(), subject, body);
            logger.info("Order confirmation email sent to {}", client.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send order confirmation email to {}: {}", client.getEmail(), e.getMessage());
        }

        return mapOrderToDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(UUID id) {
        logger.debug("Fetching order by ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + id));
        return mapOrderToDto(order);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderByIdAndValidateUser(UUID id, String userEmail, Collection<? extends GrantedAuthority> authorities) {
        logger.debug("Fetching order ID {} for user {}", id, userEmail);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + id));

        boolean isAdmin = authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN"));

        if (!isAdmin && (order.getClient() == null || !order.getClient().getEmail().equals(userEmail))) {
            logger.warn("Access denied for user {} attempting to access order {} owned by {}", userEmail, id, (order.getClient() != null ? order.getClient().getEmail() : "null"));
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to view this order.");
        }

        return mapOrderToDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrders() {
        logger.debug("Fetching all orders (ADMIN operation)");
        List<Order> orders = orderRepository.findAll();
        return mapOrderListToDtoList(orders);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByUserEmail(String email) {
        logger.debug("Fetching orders for user email: {}", email);
        User client = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
        List<Order> orders = orderRepository.findByClient(client);
        return mapOrderListToDtoList(orders);
    }

    public OrderDto updateOrderStatus(UUID id, String newStatus) {
        logger.info("Attempting to update status for order {} to {}", id, newStatus);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + id));

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        logger.info("Successfully updated status for order {} to {}", id, newStatus);

        return mapOrderToDto(updatedOrder);
    }
}