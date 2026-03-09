package com.mthree.orderbookdemo.services;

import com.mthree.orderbookdemo.models.Order;
import com.mthree.orderbookdemo.models.OrderType;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderValidationService {

    private static final BigDecimal MAX_ORDER_VALUE = new BigDecimal("1000000");
    private static final int MAX_QUANTITY = 10000;
    private static final List<String> SUPPORTED_SYMBOLS = List.of("AAPL", "GOOGL", "MSFT", "AMZN");

    public List<String> validateOrder(Order order) {
        List<String> violations = new ArrayList<>();

        // Symbol validation
        if (order.getSymbol() == null || order.getSymbol().trim().isEmpty()) {
            violations.add("Symbol is required");
        } else if (!SUPPORTED_SYMBOLS.contains(order.getSymbol())) {
            violations.add("Unsupported symbol: " + order.getSymbol());
        }

        // Order type validation
        if (order.getType() == null) {
            violations.add("Order type is required");
        }

        // Price validation
        if (order.getPrice() == null) {
            violations.add("Price is required");
        } else if (order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            violations.add("Price must be positive");
        }

        // Quantity validation
        if (order.getQuantity() <= 0) {
            violations.add("Quantity must be positive");
        } else if (order.getQuantity() > MAX_QUANTITY) {
            violations.add("Quantity exceeds maximum: " + MAX_QUANTITY);
        }

        // Order value validation
        if (order.getPrice() != null && order.getQuantity() > 0) {
            BigDecimal orderValue = order.getPrice().multiply(new BigDecimal(order.getQuantity()));
            if (orderValue.compareTo(MAX_ORDER_VALUE) > 0) {
                violations.add("Order value exceeds maximum: " + MAX_ORDER_VALUE);
            }
        }

        return violations;
    }

    public boolean isMarketOpen() {
        // Simple market hours check (9:30 AM - 4:00 PM ET)
        // In real implementation, you'd use a proper time service
        return true; // Simplified for demo
    }

    public boolean isValidOrderType(OrderType type) {
        return type != null && (type == OrderType.BUY || type == OrderType.SELL);
    }
}
