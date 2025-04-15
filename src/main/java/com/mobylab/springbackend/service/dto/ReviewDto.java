package com.mobylab.springbackend.service.dto;


import java.util.UUID;
import java.time.LocalDateTime;

public class ReviewDto {
    private UUID id;
    private Integer rating;
    private String comment;
    private LocalDateTime reviewDate;
    private String reviewerEmail;
    private UUID dishId;

    public UUID getId() {
        return id;
    }

    public Integer getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getReviewDate() {
        return reviewDate;
    }

    public String getReviewerEmail() {
        return reviewerEmail;
    }

    public UUID getDishId() {
        return dishId;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setReviewDate(LocalDateTime reviewDate) {
        this.reviewDate = reviewDate;
    }

    public void setReviewerEmail(String reviewerEmail) {
        this.reviewerEmail = reviewerEmail;
    }

    public void setDishId(UUID dishId) {
        this.dishId = dishId;
    }

    // Getters & Setters
}
