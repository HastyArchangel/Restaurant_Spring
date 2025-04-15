package com.mobylab.springbackend.controller;

import com.mobylab.springbackend.service.ReviewService;
import com.mobylab.springbackend.service.dto.ReviewDto;
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
@RequestMapping("/api/v1/reviews")
public class ReviewController implements SecuredRestController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewDto> addReview(@RequestBody ReviewDto reviewDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();

        reviewDto.setReviewerEmail(currentUserEmail);
        logger.info("User '{}' adding review for dish ID {}", currentUserEmail, reviewDto.getDishId());

        try {
            ReviewDto createdReview = reviewService.addReview(reviewDto);
            logger.info("Successfully added review with ID {} by user '{}'", createdReview.getId(), currentUserEmail);
            return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            logger.warn("Failed to add review for user '{}': {}", currentUserEmail, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e); // Dish or User not found
        }
    }

    @GetMapping("/dish/{dishId}")
    public ResponseEntity<List<ReviewDto>> getReviewsForDish(@PathVariable UUID dishId) {
        logger.info("Request received for reviews of dish ID {}", dishId);
        try {
            List<ReviewDto> reviews = reviewService.getReviewsByDishId(dishId);
            logger.info("Returning {} reviews for dish ID {}", reviews.size(), dishId);
            return ResponseEntity.ok(reviews);
        } catch (EntityNotFoundException e) {
            logger.warn("Cannot get reviews for non-existent dish ID {}", dishId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @GetMapping("/my-reviews")
    public ResponseEntity<List<ReviewDto>> getMyReviews() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        logger.info("User '{}' requesting their reviews", currentUserEmail);
        try {
            List<ReviewDto> reviews = reviewService.getReviewsByUserEmail(currentUserEmail);
            logger.info("Returning {} reviews for user '{}'", reviews.size(), currentUserEmail);
            return ResponseEntity.ok(reviews);
        } catch (EntityNotFoundException e) {
            logger.error("Authenticated user '{}' not found in database.", currentUserEmail);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve user data.", e);
        }
    }

    /**
     * Deletes a review.
     * Requires ADMIN authority OR the user must be the author of the review.
     * Uses @PreAuthorize with a call to the service bean method.
     *
     * @param id The UUID of the review to delete.
     * @return ResponseEntity with status NO_CONTENT if successful.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or @reviewService.isOwner(#id, principal.username)")
    public ResponseEntity<Void> deleteReview(@PathVariable UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        logger.info("User '{}' requesting deletion of review ID {}", currentUserEmail, id);

        try {
            reviewService.deleteReview(id); // Create this simple method in service if preferred
            logger.info("Successfully deleted review ID {} (authorized via @PreAuthorize)", id);
            return ResponseEntity.noContent().build();

        } catch (EntityNotFoundException e) {
            logger.warn("Review ID {} not found for deletion by user {}", id, currentUserEmail);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
}