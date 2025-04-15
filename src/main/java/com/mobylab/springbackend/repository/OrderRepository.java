package com.mobylab.springbackend.repository;

import com.mobylab.springbackend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import com.mobylab.springbackend.entity.User;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByClient(User client);
}
