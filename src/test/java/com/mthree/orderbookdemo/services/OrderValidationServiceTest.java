package com.mthree.orderbookdemo.services;


import com.mthree.orderbookdemo.models.Order;
import com.mthree.orderbookdemo.models.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class OrderValidationServiceTest {

    private OrderValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new OrderValidationService();
    }

    @Test
    void shouldValidateValidOrder() {
        Order order = createValidOrder();

        List<String> violations = validationService.validateOrder(order);

        assertTrue(violations.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("provideInvalidOrders")
    void shouldDetectInvalidOrders(Order order, String expectedViolation) {
        List<String> violations = validationService.validateOrder(order);

        assertFalse(violations.isEmpty());
        assertTrue(violations.contains(expectedViolation));
    }

    private static Stream<Arguments> provideInvalidOrders() {
        return Stream.of(
                Arguments.of(
                        createOrder(null, OrderType.BUY, new BigDecimal("100"), 10),
                        "Symbol is required"
                ),
                Arguments.of(
                        createOrder("INVALID", OrderType.BUY, new BigDecimal("100"), 10),
                        "Unsupported symbol: INVALID"
                ),
                Arguments.of(
                        createOrder("AAPL", null, new BigDecimal("100"), 10),
                        "Order type is required"
                ),
                Arguments.of(
                        createOrder("AAPL", OrderType.BUY, null, 10),
                        "Price is required"
                ),
                Arguments.of(
                        createOrder("AAPL", OrderType.BUY, BigDecimal.ZERO, 10),
                        "Price must be positive"
                ),
                Arguments.of(
                        createOrder("AAPL", OrderType.BUY, new BigDecimal("100"), 0),
                        "Quantity must be positive"
                ),
                Arguments.of(
                        createOrder("AAPL", OrderType.BUY, new BigDecimal("100"), 20000),
                        "Quantity exceeds maximum: 10000"
                )
        );
    }

    @Test
    void shouldValidateOrderType() {
        assertTrue(validationService.isValidOrderType(OrderType.BUY));
        assertTrue(validationService.isValidOrderType(OrderType.SELL));
        assertFalse(validationService.isValidOrderType(null));
    }

    private static Order createValidOrder() {
        return createOrder("AAPL", OrderType.BUY, new BigDecimal("150.50"), 100);
    }

    private static Order createOrder(String symbol, OrderType type, BigDecimal price, int quantity) {
        Order order = new Order();
        order.setSymbol(symbol);
        order.setType(type);
        order.setPrice(price);
        order.setQuantity(quantity);
        return order;
    }
}