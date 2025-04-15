package com.mobylab.springbackend.service;

import com.mobylab.springbackend.entity.Dish;
import com.mobylab.springbackend.entity.Review;
import com.mobylab.springbackend.entity.User;
import com.mobylab.springbackend.exception.BadRequestException;
import com.mobylab.springbackend.repository.DishRepository;
import com.mobylab.springbackend.repository.ReviewRepository;
import com.mobylab.springbackend.repository.UserRepository;
import com.mobylab.springbackend.service.dto.ReviewDto;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final DishRepository dishRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         DishRepository dishRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.dishRepository = dishRepository;
    }

    // --- Manual Mapping Helper Methods ---
    private ReviewDto mapReviewToDto(Review review) {
        if (review == null) {
            return null;
        }
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setReviewDate(review.getReviewDate());

        if (review.getReviewer() != null) {
            dto.setReviewerEmail(review.getReviewer().getEmail());
        }
        if (review.getDish() != null) {
            dto.setDishId(review.getDish().getId());
        }
        return dto;
    }

    private List<ReviewDto> mapReviewListToDtoList(List<Review> reviews) {
        if (reviews == null) {
            return new ArrayList<>();
        }
        return reviews.stream()
                .map(this::mapReviewToDto)
                .collect(Collectors.toList());
    }
    // --- End Manual Mapping ---

    public ReviewDto addReview(ReviewDto reviewDto) {
        logger.debug("Attempting to add review for dish {} by user {}", reviewDto.getDishId(), reviewDto.getReviewerEmail());

        User reviewer = userRepository.findUserByEmail(reviewDto.getReviewerEmail())
                .orElseThrow(() -> new EntityNotFoundException("Reviewer user not found with email: " + reviewDto.getReviewerEmail()));

        Dish dish = dishRepository.findById(reviewDto.getDishId())
                .orElseThrow(() -> new EntityNotFoundException("Dish not found with ID: " + reviewDto.getDishId()));

        Review review = new Review();
        review.setReviewer(reviewer);
        review.setDish(dish);
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());
        review.setReviewDate(LocalDateTime.now());

        Review savedReview = reviewRepository.save(review);
        logger.info("Review {} added successfully for dish {} by user {}", savedReview.getId(), dish.getId(), reviewer.getEmail());

        return mapReviewToDto(savedReview); // Use manual mapping
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsByDishId(UUID dishId) {
        logger.debug("Fetching reviews for dish ID: {}", dishId);
        if (!dishRepository.existsById(dishId)) {
            throw new EntityNotFoundException("Dish not found with ID: " + dishId);
        }
        List<Review> reviews = reviewRepository.findByDishId(dishId);
        return mapReviewListToDtoList(reviews); // Use manual mapping
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsByUserEmail(String email) {
        logger.debug("Fetching reviews by user email: {}", email);
        User reviewer = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
        List<Review> reviews = reviewRepository.findByReviewer(reviewer);
        return mapReviewListToDtoList(reviews); // Use manual mapping
    }

    public boolean deleteReviewIfAllowed(UUID reviewId, String requesterEmail, Collection<? extends GrantedAuthority> authorities) {
        logger.info("User {} attempting to delete review {}", requesterEmail, reviewId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found with ID: " + reviewId));

        boolean isAdmin = authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN"));
        boolean isOwner = review.getReviewer() != null && review.getReviewer().getEmail().equals(requesterEmail);

        if (isAdmin || isOwner) {
            reviewRepository.delete(review);
            logger.info("Review {} deleted successfully by {}", reviewId, requesterEmail);
            return true;
        } else {
            logger.warn("Authorization failed: User {} cannot delete review {} owned by {}",
                    requesterEmail, reviewId, (review.getReviewer() != null ? review.getReviewer().getEmail() : "null"));
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to delete this review.");
        }
    }

    @Transactional(readOnly = true)
    public boolean isOwner(UUID reviewId, String userEmail) {
        if (userEmail == null) return false;
        return reviewRepository.findById(reviewId)
                .map(review -> review.getReviewer() != null && review.getReviewer().getEmail().equals(userEmail))
                .orElse(false);
    }

    public void deleteReview(UUID reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new EntityNotFoundException("Review not found with ID: " + reviewId);
        }
        reviewRepository.deleteById(reviewId);
    }
}