package com.example.orderbookdemo.services;

import com.mthree.orderbookdemo.models.Order;
import com.mthree.orderbookdemo.models.OrderType;
import com.mthree.orderbookdemo.models.Trade;
import com.mthree.orderbookdemo.services.OrderBookStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookStatisticsServiceTest {

    private OrderBookStatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        statisticsService = new OrderBookStatisticsService();
    }

    @Test
    void shouldCalculateTotalVolume() {
        List<Trade> trades = createSampleTrades();

        Map<String, Object> stats = statisticsService.calculateStatistics(trades, Collections.emptyList());

        assertEquals(2, stats.get("totalTrades"));
        assertEquals(new BigDecimal("2600.00"), stats.get("totalVolume"));
    }

    @Test
    void shouldCalculateVWAP() {
        List<Trade> trades = createSampleTrades();

        Map<String, Object> stats = statisticsService.calculateStatistics(trades, Collections.emptyList());

        // VWAP = (10*100 + 20*80) / (10+20) = (1000 + 1600) / 30 = 2600/30 = 86.67
        assertEquals(new BigDecimal("86.67"), stats.get("vwap"));
    }

    @Test
    void shouldCalculateOrderImbalance() {
        List<Order> orders = Arrays.asList(
                createOrder(OrderType.BUY),
                createOrder(OrderType.BUY),
                createOrder(OrderType.SELL)
        );

        Map<String, Object> stats = statisticsService.calculateStatistics(
                Collections.emptyList(),
                orders
        );

        // (2-1)/3 = 0.333...
        assertEquals(0.3333333333333333, (double) stats.get("orderImbalance"), 0.0001);
    }

    @Test
    void shouldGetPriceLevelsWithMixedOrders() {
        List<Order> orders = Arrays.asList(
                createOrderWithPriceAndQuantity(OrderType.BUY, new BigDecimal("150.50"), 25),
                createOrderWithPriceAndQuantity(OrderType.BUY, new BigDecimal("150.50"), 10),
                createOrderWithPriceAndQuantity(OrderType.BUY, new BigDecimal("149.75"), 30),
                createOrderWithPriceAndQuantity(OrderType.SELL, new BigDecimal("151.25"), 20),
                createOrderWithPriceAndQuantity(OrderType.SELL, new BigDecimal("151.25"), 5),
                createOrderWithPriceAndQuantity(OrderType.SELL, new BigDecimal("152.00"), 15)
        );

        Map<String, Object> allLevels = statisticsService.getPriceLevels(orders, 3);

        // Should get top 3 price levels by price (highest first)
        assertEquals(3, allLevels.size());

        // Check the values (order should be: 152.00, 151.25, 150.50)
        Iterator<Map.Entry<String, Object>> iterator = allLevels.entrySet().iterator();

        Map.Entry<String, Object> first = iterator.next();
        assertEquals("152.00", first.getKey());
        assertEquals(15, first.getValue());

        Map.Entry<String, Object> second = iterator.next();
        assertEquals("151.25", second.getKey());
        assertEquals(25, second.getValue()); // 20 + 5

        Map.Entry<String, Object> third = iterator.next();
        assertEquals("150.50", third.getKey());
        assertEquals(35, third.getValue()); // 25 + 10
    }

    @Test
    void shouldHandleEmptyListForPriceLevels() {
        Map<String, Object> priceLevels = statisticsService.getPriceLevels(Collections.emptyList(), 5);

        assertNotNull(priceLevels);
        assertTrue(priceLevels.isEmpty());
    }

    @Test
    void shouldHandleNullListForPriceLevels() {
        Map<String, Object> priceLevels = statisticsService.getPriceLevels(null, 5);

        assertNotNull(priceLevels);
        assertTrue(priceLevels.isEmpty());
    }

    @Test
    void shouldCalculateBidAndAskLevels() {
        List<Order> buyOrders = Arrays.asList(
                createOrderWithPriceAndQuantity(OrderType.BUY, new BigDecimal("100.00"), 10),
                createOrderWithPriceAndQuantity(OrderType.BUY, new BigDecimal("99.00"), 20),
                createOrderWithPriceAndQuantity(OrderType.BUY, new BigDecimal("98.00"), 30)
        );

        List<Order> sellOrders = Arrays.asList(
                createOrderWithPriceAndQuantity(OrderType.SELL, new BigDecimal("101.00"), 15),
                createOrderWithPriceAndQuantity(OrderType.SELL, new BigDecimal("102.00"), 25),
                createOrderWithPriceAndQuantity(OrderType.SELL, new BigDecimal("103.00"), 35)
        );

        List<Order> allOrders = new ArrayList<>();
        allOrders.addAll(buyOrders);
        allOrders.addAll(sellOrders);

        Map<String, Object> stats = statisticsService.calculateStatistics(
                Collections.emptyList(),
                allOrders
        );

        // Check bid levels (should be highest prices first)
        Map<String, Object> bidLevels = (Map<String, Object>) stats.get("bidLevels");
        assertNotNull(bidLevels);
        assertEquals(3, bidLevels.size());
        assertEquals(10, bidLevels.get("100.00"));
        assertEquals(20, bidLevels.get("99.00"));
        assertEquals(30, bidLevels.get("98.00"));

        // Check ask levels (should be highest prices first)
        Map<String, Object> askLevels = (Map<String, Object>) stats.get("askLevels");
        assertNotNull(askLevels);
        assertEquals(3, askLevels.size());
        assertEquals(35, askLevels.get("103.00"));
        assertEquals(25, askLevels.get("102.00"));
        assertEquals(15, askLevels.get("101.00"));
    }

    @Test
    void shouldCalculateSpread() {
        List<Order> buyOrders = Arrays.asList(
                createOrderWithPriceAndQuantity(OrderType.BUY, new BigDecimal("100.00"), 10),
                createOrderWithPriceAndQuantity(OrderType.BUY, new BigDecimal("99.50"), 20)
        );

        List<Order> sellOrders = Arrays.asList(
                createOrderWithPriceAndQuantity(OrderType.SELL, new BigDecimal("101.00"), 15),
                createOrderWithPriceAndQuantity(OrderType.SELL, new BigDecimal("102.00"), 25)
        );

        List<Order> allOrders = new ArrayList<>();
        allOrders.addAll(buyOrders);
        allOrders.addAll(sellOrders);

        Map<String, Object> stats = statisticsService.calculateStatistics(
                Collections.emptyList(),
                allOrders
        );

        // Spread = lowest ask (101.00) - highest bid (100.00) = 1.00
        assertEquals(new BigDecimal("1.00"), stats.get("spread"));
    }

    private List<Trade> createSampleTrades() {
        List<Trade> trades = new ArrayList<>();

        Trade trade1 = new Trade();
        trade1.setPrice(new BigDecimal("100.00"));
        trade1.setQuantity(10);

        Trade trade2 = new Trade();
        trade2.setPrice(new BigDecimal("80.00"));
        trade2.setQuantity(20);

        trades.add(trade1);
        trades.add(trade2);

        return trades;
    }

    private Order createOrder(OrderType type) {
        return createOrderWithPriceAndQuantity(type, new BigDecimal("100.00"), 10);
    }

    private Order createOrderWithPriceAndQuantity(OrderType type, BigDecimal price, int quantity) {
        Order order = new Order();
        order.setType(type);
        order.setPrice(price);
        order.setQuantity(quantity);
        order.setFilledQuantity(0);
        return order;
    }
}