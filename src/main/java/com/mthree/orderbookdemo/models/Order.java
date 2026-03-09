package com.mthree.orderbookdemo.models;

import java.math.BigDecimal;
import java.util.Date;

public class Order {
    private String orderId;
    private String symbol;
    private OrderType type;
    private BigDecimal price;
    private int quantity;
    private int filledQuantity;
    private Date timestamp;

    // Constructors
    public Order() {}

    public Order(String symbol, OrderType type, BigDecimal price, int quantity) {
        this.symbol = symbol;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.filledQuantity = 0;
    }

    // Getters and setters for all fields
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public OrderType getType() { return type; }
    public void setType(OrderType type) { this.type = type; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getFilledQuantity() { return filledQuantity; }
    public void setFilledQuantity(int filledQuantity) { this.filledQuantity = filledQuantity; }

    public int getRemainingQuantity() { return quantity - filledQuantity; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}