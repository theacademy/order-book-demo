
package com.mthree.orderbookdemo.services;

import com.mthree.orderbookdemo.models.Order;
import com.mthree.orderbookdemo.models.Trade;
import com.mthree.orderbookdemo.models.OrderType;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class OrderBookService {

    private final Map<String, List<Order>> buyOrders = new ConcurrentHashMap<>();
    private final Map<String, List<Order>> sellOrders = new ConcurrentHashMap<>();
    private final List<Trade> trades = new CopyOnWriteArrayList<>();

    /**
     * Add a new order to the order book
     */
    public Order addOrder(String symbol, OrderType type, BigDecimal price, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }

        Order order = new Order(symbol, type, price, quantity);
        order.setOrderId(UUID.randomUUID().toString());
        order.setTimestamp(new Date());

        // Try to match the order first
        List<Trade> matchedTrades = matchOrder(order);

        if (order.getRemainingQuantity() > 0) {
            // Add remaining to order book
            Map<String, List<Order>> targetMap = type == OrderType.BUY ? buyOrders : sellOrders;
            targetMap.computeIfAbsent(symbol, k -> new CopyOnWriteArrayList<>()).add(order);
        }

        trades.addAll(matchedTrades);
        return order;
    }

    /**
     * Match an order against existing orders
     */
    private List<Trade> matchOrder(Order newOrder) {
        List<Trade> matchedTrades = new ArrayList<>();
        Map<String, List<Order>> oppositeMap = newOrder.getType() == OrderType.BUY ? sellOrders : buyOrders;

        List<Order> oppositeOrders = oppositeMap.getOrDefault(newOrder.getSymbol(), new ArrayList<>());

        // Sort opposite orders appropriately
        oppositeOrders.sort((o1, o2) -> {
            if (newOrder.getType() == OrderType.BUY) {
                // For buy orders, match with lowest sell price first
                return o1.getPrice().compareTo(o2.getPrice());
            } else {
                // For sell orders, match with highest buy price first
                return o2.getPrice().compareTo(o1.getPrice());
            }
        });

        Iterator<Order> iterator = oppositeOrders.iterator();
        while (iterator.hasNext() && newOrder.getRemainingQuantity() > 0) {
            Order oppositeOrder = iterator.next();

            // Check if prices cross
            if (newOrder.getType() == OrderType.BUY) {
                if (newOrder.getPrice().compareTo(oppositeOrder.getPrice()) < 0) {
                    break; // No more matches possible
                }
            } else {
                if (newOrder.getPrice().compareTo(oppositeOrder.getPrice()) > 0) {
                    break; // No more matches possible
                }
            }

            // Calculate match quantity
            int matchQuantity = Math.min(newOrder.getRemainingQuantity(), oppositeOrder.getRemainingQuantity());
            BigDecimal matchPrice = oppositeOrder.getPrice();

            // Create trade
            Trade trade = new Trade();
            trade.setTradeId(UUID.randomUUID().toString());
            trade.setSymbol(newOrder.getSymbol());
            trade.setBuyOrderId(newOrder.getType() == OrderType.BUY ? newOrder.getOrderId() : oppositeOrder.getOrderId());
            trade.setSellOrderId(newOrder.getType() == OrderType.SELL ? newOrder.getOrderId() : oppositeOrder.getOrderId());
            trade.setPrice(matchPrice);
            trade.setQuantity(matchQuantity);
            trade.setTimestamp(new Date());

            matchedTrades.add(trade);

            // Update orders
            newOrder.setFilledQuantity(newOrder.getFilledQuantity() + matchQuantity);
            oppositeOrder.setFilledQuantity(oppositeOrder.getFilledQuantity() + matchQuantity);

            // Remove fully filled opposite order
            if (oppositeOrder.getRemainingQuantity() == 0) {
                iterator.remove();
            }
        }

        return matchedTrades;
    }

    /**
     * Cancel an existing order
     */
    public boolean cancelOrder(String orderId) {
        // Check buy orders
        for (List<Order> orders : buyOrders.values()) {
            if (orders.removeIf(order -> order.getOrderId().equals(orderId))) {
                return true;
            }
        }

        // Check sell orders
        for (List<Order> orders : sellOrders.values()) {
            if (orders.removeIf(order -> order.getOrderId().equals(orderId))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get order book for a symbol
     */
    public Map<String, Object> getOrderBook(String symbol) {
        Map<String, Object> orderBook = new HashMap<>();

        List<Order> buys = buyOrders.getOrDefault(symbol, new ArrayList<>());
        List<Order> sells = sellOrders.getOrDefault(symbol, new ArrayList<>());

        // Sort and aggregate by price level
        Map<BigDecimal, Integer> buyLevels = aggregateOrdersByPrice(buys);
        Map<BigDecimal, Integer> sellLevels = aggregateOrdersByPrice(sells);

        orderBook.put("symbol", symbol);
        orderBook.put("bids", buyLevels);
        orderBook.put("asks", sellLevels);
        orderBook.put("spread", calculateSpread(buyLevels, sellLevels));

        return orderBook;
    }

    private Map<BigDecimal, Integer> aggregateOrdersByPrice(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getRemainingQuantity() > 0)
                .collect(Collectors.groupingBy(
                        Order::getPrice,
                        Collectors.summingInt(Order::getRemainingQuantity)
                ));
    }

    private BigDecimal calculateSpread(Map<BigDecimal, Integer> buys, Map<BigDecimal, Integer> sells) {
        if (buys.isEmpty() || sells.isEmpty()) {
            return null;
        }

        BigDecimal highestBid = buys.keySet().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal lowestAsk = sells.keySet().stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        return lowestAsk.subtract(highestBid);
    }

    /**
     * Get recent trades
     */
    public List<Trade> getRecentTrades(String symbol, int limit) {
        return trades.stream()
                .filter(t -> t.getSymbol().equals(symbol))
                .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}