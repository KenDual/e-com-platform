package com.maiphuhai.model;

import java.sql.Timestamp;

public class Order {
    private int OrderId;
    private int UserId;
    private Timestamp OrderDate;
    private double OrderAmount;
    private int PaymentStatus;
    private int DeliveryStatus;

    public Order(int orderId, int userId, Timestamp orderDate, double orderAmount, int paymentStatus, int deliveryStatus) {
        OrderId = orderId;
        UserId = userId;
        OrderDate = orderDate;
        OrderAmount = orderAmount;
        PaymentStatus = paymentStatus;
        DeliveryStatus = deliveryStatus;
    }

    public int getOrderId() {
        return OrderId;
    }

    public void setOrderId(int orderId) {
        OrderId = orderId;
    }

    public int getUserId() {
        return UserId;
    }

    public void setUserId(int userId) {
        UserId = userId;
    }

    public Timestamp getOrderDate() {
        return OrderDate;
    }

    public void setOrderDate(Timestamp orderDate) {
        OrderDate = orderDate;
    }

    public double getOrderAmount() {
        return OrderAmount;
    }

    public void setOrderAmount(double orderAmount) {
        OrderAmount = orderAmount;
    }

    public int getPaymentStatus() {
        return PaymentStatus;
    }

    public void setPaymentStatus(int paymentStatus) {
        PaymentStatus = paymentStatus;
    }

    public int getDeliveryStatus() {
        return DeliveryStatus;
    }

    public void setDeliveryStatus(int deliveryStatus) {
        DeliveryStatus = deliveryStatus;
    }
}
