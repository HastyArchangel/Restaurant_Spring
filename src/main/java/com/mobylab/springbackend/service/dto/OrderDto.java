package com.mobylab.springbackend.service.dto;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {
    private UUID id;
    private LocalDateTime orderDate;
    private String status;
    private String clientEmail;
    private List<UUID> dishIds;

    public UUID getId() {
        return id;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public String getStatus() {
        return status;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public List<UUID> getDishIds() {
        return dishIds;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public void setDishIds(List<UUID> dishIds) {
        this.dishIds = dishIds;
    }

    // Getters & Setters
}
