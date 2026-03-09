package com.mthree.orderbookdemo.services;

import com.mthree.orderbookdemo.models.Order;
import com.mthree.orderbookdemo.models.OrderType;
import com.mthree.orderbookdemo.models.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookServiceTest {

    private OrderBookService orderBookService;

    @BeforeEach
    void setUp() {
        orderBookService = new OrderBookService();
    }

    @Nested
    @DisplayName("Order Addition Tests")
    class OrderAdditionTests {

        @Test
        @DisplayName("Should successfully add a buy order")
        void shouldAddBuyOrder() {
            Order order = orderBookService.addOrder(
                    "AAPL",
                    OrderType.BUY,
                    new BigDecimal("150.50"),
                    100
            );

            assertNotNull(order);
            assertNotNull(order.getOrderId());
            assertEquals("AAPL", order.getSymbol());
            assertEquals(OrderType.BUY, order.getType());
            assertEquals(new BigDecimal("150.50"), order.getPrice());
            assertEquals(100, order.getQuantity());
            assertEquals(100, order.getRemainingQuantity());
            assertEquals(0, order.getFilledQuantity());
        }

        @Test
        @DisplayName("Should throw exception for invalid quantity")
        void shouldThrowExceptionForInvalidQuantity() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> orderBookService.addOrder("AAPL", OrderType.BUY, new BigDecimal("150.50"), 0)
            );

            assertEquals("Quantity must be positive", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for invalid price")
        void shouldThrowExceptionForInvalidPrice() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> orderBookService.addOrder("AAPL", OrderType.BUY, BigDecimal.ZERO, 100)
            );

            assertEquals("Price must be positive", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Order Matching Tests")
    class OrderMatchingTests {

        @Test
        @DisplayName("Should match buy order with existing sell order")
        void shouldMatchBuyOrderWithSellOrder() {
            // Add a sell order first
            orderBookService.addOrder("AAPL", OrderType.SELL, new BigDecimal("150.00"), 50);

            // Add a matching buy order
            Order buyOrder = orderBookService.addOrder("AAPL", OrderType.BUY, new BigDecimal("151.00"), 30);

            assertEquals(0, buyOrder.getRemainingQuantity());
            assertEquals(30, buyOrder.getFilledQuantity());

            // Check trades were created
            List<Trade> trades = orderBookService.getRecentTrades("AAPL", 10);
            assertEquals(1, trades.size());

            Trade trade = trades.get(0);
            assertEquals("AAPL", trade.getSymbol());
            assertEquals(new BigDecimal("150.00"), trade.getPrice());
            assertEquals(30, trade.getQuantity());
        }


        @Test
        @DisplayName("Should not match orders when prices don't cross")
        void shouldNotMatchWhenPricesDontCross() {
            // Add a sell order at high price
            orderBookService.addOrder("AAPL", OrderType.SELL, new BigDecimal("160.00"), 50);

            // Add a buy order at lower price
            Order buyOrder = orderBookService.addOrder("AAPL", OrderType.BUY, new BigDecimal("150.00"), 30);

            assertEquals(30, buyOrder.getRemainingQuantity());
            assertEquals(0, buyOrder.getFilledQuantity());

            List<Trade> trades = orderBookService.getRecentTrades("AAPL", 10);
            assertTrue(trades.isEmpty());
        }
    }

    @Nested
    @DisplayName("Order Cancellation Tests")
    class OrderCancellationTests {

        @Test
        @DisplayName("Should cancel existing order")
        void shouldCancelExistingOrder() {
            Order order = orderBookService.addOrder("AAPL", OrderType.BUY, new BigDecimal("150.00"), 100);

            boolean cancelled = orderBookService.cancelOrder(order.getOrderId());
            assertTrue(cancelled);

            Map<String, Object> orderBook = orderBookService.getOrderBook("AAPL");
            Map<BigDecimal, Integer> bids = (Map<BigDecimal, Integer>) orderBook.get("bids");
            assertTrue(bids.isEmpty());
        }

        @Test
        @DisplayName("Should return false for non-existent order")
        void shouldReturnFalseForNonExistentOrder() {
            boolean cancelled = orderBookService.cancelOrder("non-existent-id");
            assertFalse(cancelled);
        }
    }
}