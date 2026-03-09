package com.mthree.orderbookdemo.services;

import com.mthree.orderbookdemo.models.Order;
import com.mthree.orderbookdemo.models.OrderType;
import com.mthree.orderbookdemo.models.Trade;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderBookStatisticsService {

    public Map<String, Object> calculateStatistics(List<Trade> trades, List<Order> activeOrders) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalTrades", trades.size());
        stats.put("totalVolume", calculateTotalVolume(trades));
        stats.put("vwap", calculateVWAP(trades));
        stats.put("activeOrders", activeOrders.size());
        stats.put("orderImbalance", calculateOrderImbalance(activeOrders));
        stats.put("averageOrderSize", calculateAverageOrderSize(activeOrders));

        // Price levels with string keys for JSON compatibility
        stats.put("priceLevels", getPriceLevels(activeOrders, 5));

        // Separate bid and ask levels
        List<Order> buyOrders = activeOrders.stream()
                .filter(o -> o.getType() == OrderType.BUY)
                .collect(Collectors.toList());
        List<Order> sellOrders = activeOrders.stream()
                .filter(o -> o.getType() == OrderType.SELL)
                .collect(Collectors.toList());

        stats.put("bidLevels", getPriceLevels(buyOrders, 5));
        stats.put("askLevels", getPriceLevels(sellOrders, 5));

        // Calculate spread
        BigDecimal spread = calculateSpread(buyOrders, sellOrders);
        if (spread != null) {
            stats.put("spread", spread);
        }

        return stats;
    }

    private BigDecimal calculateTotalVolume(List<Trade> trades) {
        return trades.stream()
                .map(t -> t.getPrice().multiply(new BigDecimal(t.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateVWAP(List<Trade> trades) {
        if (trades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalValue = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (Trade trade : trades) {
            totalValue = totalValue.add(trade.getPrice().multiply(new BigDecimal(trade.getQuantity())));
            totalQuantity += trade.getQuantity();
        }

        if (totalQuantity == 0) {
            return BigDecimal.ZERO;
        }

        return totalValue.divide(new BigDecimal(totalQuantity), 2, RoundingMode.HALF_UP);
    }

    private double calculateOrderImbalance(List<Order> orders) {
        long buyCount = orders.stream().filter(o -> o.getType() == OrderType.BUY).count();
        long sellCount = orders.stream().filter(o -> o.getType() == OrderType.SELL).count();

        long total = buyCount + sellCount;
        if (total == 0) {
            return 0.0;
        }

        return (double) (buyCount - sellCount) / total;
    }

    private double calculateAverageOrderSize(List<Order> orders) {
        return orders.stream()
                .mapToInt(Order::getQuantity)
                .average()
                .orElse(0.0);
    }

    /**
     * Get price levels as a map with string keys (for JSON serialization)
     */
    public Map<String, Object> getPriceLevels(List<Order> orders, int levels) {
        if (orders == null || orders.isEmpty()) {
            return new LinkedHashMap<>();
        }

        // Group by price and sum quantities
        Map<BigDecimal, Integer> priceLevels = orders.stream()
                .filter(o -> o.getRemainingQuantity() > 0)
                .collect(Collectors.groupingBy(
                        Order::getPrice,
                        Collectors.summingInt(Order::getRemainingQuantity)
                ));

        // Sort, limit, and convert BigDecimal keys to String
        return priceLevels.entrySet().stream()
                .sorted(Map.Entry.<BigDecimal, Integer>comparingByKey().reversed())
                .limit(levels)
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(),  // Convert BigDecimal to String
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Calculate bid-ask spread
     */
    private BigDecimal calculateSpread(List<Order> buyOrders, List<Order> sellOrders) {
        if (buyOrders.isEmpty() || sellOrders.isEmpty()) {
            return null;
        }

        BigDecimal highestBid = buyOrders.stream()
                .map(Order::getPrice)
                .max(BigDecimal::compareTo)
                .orElse(null);

        BigDecimal lowestAsk = sellOrders.stream()
                .map(Order::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(null);

        if (highestBid == null || lowestAsk == null) {
            return null;
        }

        return lowestAsk.subtract(highestBid);
    }
}
