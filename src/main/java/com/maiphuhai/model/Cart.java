package com.maiphuhai.model;

import java.sql.Timestamp;

public class Cart {
    private int CartId;
    private int UserId;
    private Timestamp CreatedAt;

    public Cart(int cartId, int userId, Timestamp createdAt) {
        CartId = cartId;
        UserId = userId;
        CreatedAt = createdAt;
    }

    public int getCartId() {
        return CartId;
    }

    public void setCartId(int cartId) {
        CartId = cartId;
    }

    public int getUserId() {
        return UserId;
    }

    public void setUserId(int userId) {
        UserId = userId;
    }

    public Timestamp getCreatedAt() {
        return CreatedAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        CreatedAt = createdAt;
    }
}
