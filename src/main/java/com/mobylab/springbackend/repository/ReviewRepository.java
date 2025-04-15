package com.mobylab.springbackend.repository;

import com.mobylab.springbackend.entity.Review;
import com.mobylab.springbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByDishId(UUID dishId);

    List<Review> findByReviewer(User reviewer);
}
