package com.maiphuhai.model;

import java.sql.Timestamp;

public class Review {
    private int ReviewId;
    private int ProductId;
    private int UserId;
    private int Rating;
    private String Title;
    private String Content;
    private Timestamp CreatedAt;

    public Review(){}

    public Review(int reviewId, int productId, int userId, int rating, String title, String content, Timestamp createdAt) {
        ReviewId = reviewId;
        ProductId = productId;
        UserId = userId;
        Rating = rating;
        Title = title;
        Content = content;
        CreatedAt = createdAt;
    }

    public int getReviewId() {
        return ReviewId;
    }

    public void setReviewId(int reviewId) {
        ReviewId = reviewId;
    }

    public int getProductId() {
        return ProductId;
    }

    public void setProductId(int productId) {
        ProductId = productId;
    }

    public int getUserId() {
        return UserId;
    }

    public void setUserId(int userId) {
        UserId = userId;
    }

    public int getRating() {
        return Rating;
    }

    public void setRating(int rating) {
        Rating = rating;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getContent() {
        return Content;
    }

    public void setContent(String content) {
        Content = content;
    }

    public Timestamp getCreatedAt() {
        return CreatedAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        CreatedAt = createdAt;
    }
}
