package com.example.investment_portfolio_tracker.service;


import com.example.investment_portfolio_tracker.model.*;
import com.example.investment_portfolio_tracker.repository.PositionRepository;
import com.example.investment_portfolio_tracker.repository.StockRepository;
import com.example.investment_portfolio_tracker.repository.TransactionRepository;
import com.example.investment_portfolio_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Get all positions for a user
     */
    @Transactional(readOnly = true)
    public List<Position> getUserPositions(Long userId) {
        return positionRepository.findByUserId(userId);
    }

    /**
     * Get a specific position
     */
    @Transactional(readOnly = true)
    public Optional<Position> getPosition(Long positionId) {
        return positionRepository.findById(positionId);
    }

    /**
     * Get total portfolio value
     */
    @Transactional(readOnly = true)
    public BigDecimal getPortfolioValue(Long userId) {
        Double value = positionRepository.getTotalPortfolioValue(userId);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value);
    }

    /**
     * Get all transactions for a user
     */
    @Transactional(readOnly = true)
    public List<Transaction> getUserTransactions(Long userId) {
        return transactionRepository.findByUserId(userId);
    }

    /**
     * Get paginated transactions for a user
     */
    @Transactional(readOnly = true)
    public List<Transaction> getPaginatedTransactions(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId, pageable).getContent();
    }

    /**
     * Get transactions for a specific stock
     */
    @Transactional(readOnly = true)
    public List<Transaction> getStockTransactions(Long userId, Long stockId) {
        return transactionRepository.findByUserIdAndStockIdOrderByDateDesc(userId, stockId);
    }

    /**
     * Get transactions in a date range
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsInDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    /**
     * Execute a BUY transaction
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Transaction buyStock(Long userId, String ticker, BigDecimal quantity, BigDecimal price, BigDecimal fee, LocalDateTime transactionDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Stock stock = stockRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + ticker));

        // Find existing position or create a new one
        Optional<Position> existingPosition = positionRepository.findByUserAndStock(user, stock);
        Position position;

        if (existingPosition.isPresent()) {
            position = existingPosition.get();

            // Calculate new average cost
            BigDecimal totalShares = position.getQuantity().add(quantity);
            BigDecimal existingValue = position.getQuantity().multiply(position.getAverageCost());
            BigDecimal newValue = quantity.multiply(price);
            BigDecimal newAverageCost = existingValue.add(newValue).divide(totalShares, 4, RoundingMode.HALF_UP);

            // Update position
            position.setQuantity(totalShares);
            position.setAverageCost(newAverageCost);
            position.setLastTransaction(transactionDate);
        } else {
            // Create new position
            position = new Position();
            position.setUser(user);
            position.setStock(stock);
            position.setQuantity(quantity);
            position.setAverageCost(price);
            position.setFirstPurchased(transactionDate);
            position.setLastTransaction(transactionDate);
        }

        position = positionRepository.save(position);
        log.info("Updated position for user {} and stock {}: {} shares at avg cost {}",
                userId, ticker, position.getQuantity(), position.getAverageCost());

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setStock(stock);
        transaction.setPosition(position);
        transaction.setTransactionType(TransactionType.BUY);
        transaction.setQuantity(quantity);
        transaction.setPrice(price);
        transaction.setFee(fee);
        transaction.setTransactionDate(transactionDate);

        transaction = transactionRepository.save(transaction);
        log.info("Created BUY transaction for user {} and stock {}: {} shares at {} for a total of {}",
                userId, ticker, quantity, price, transaction.getValue());

        return transaction;
    }

    /**
     * Execute a SELL transaction
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Transaction sellStock(Long userId, String ticker, BigDecimal quantity, BigDecimal price, BigDecimal fee, LocalDateTime transactionDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Stock stock = stockRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + ticker));

        // Find existing position
        Position position = positionRepository.findByUserAndStock(user, stock)
                .orElseThrow(() -> new IllegalArgumentException("No position found for " + ticker));

        // Verify sufficient quantity
        if (position.getQuantity().compareTo(quantity) < 0) {
            throw new IllegalArgumentException("Insufficient shares to sell");
        }

        // Update position
        BigDecimal remainingShares = position.getQuantity().subtract(quantity);
        position.setQuantity(remainingShares);
        position.setLastTransaction(transactionDate);

        // If completely sold, we could delete the position, but keeping it for history
        if (remainingShares.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Position for user {} and stock {} completely sold", userId, ticker);
            // We're keeping the position record for history
        }

        position = positionRepository.save(position);
        log.info("Updated position for user {} and stock {}: {} shares remaining", userId, ticker, remainingShares);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setStock(stock);
        transaction.setPosition(position);
        transaction.setTransactionType(TransactionType.SELL);
        transaction.setQuantity(quantity);
        transaction.setPrice(price);
        transaction.setFee(fee);
        transaction.setTransactionDate(transactionDate);

        transaction = transactionRepository.save(transaction);
        log.info("Created SELL transaction for user {} and stock {}: {} shares at {} for a total of {}",
                userId, ticker, quantity, price, transaction.getValue());

        return transaction;
    }

    /**
     * Calculate portfolio performance metrics
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> calculatePerformanceMetrics(Long userId) {
        List<Position> positions = positionRepository.findByUserId(userId);

        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (Position position : positions) {
            BigDecimal currentValue = position.getCurrentValue();
            if (currentValue != null) {
                totalValue = totalValue.add(currentValue);
                totalCost = totalCost.add(position.getTotalCost());
            }
        }

        BigDecimal totalGain = totalValue.subtract(totalCost);
        BigDecimal percentageReturn = BigDecimal.ZERO;

        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            percentageReturn = totalGain.multiply(BigDecimal.valueOf(100))
                    .divide(totalCost, 2, RoundingMode.HALF_UP);
        }

        // Include transaction information
        BigDecimal totalInvestment = transactionRepository.getTotalInvestmentAmount(userId);
        if (totalInvestment == null) totalInvestment = BigDecimal.ZERO;

        BigDecimal totalSales = transactionRepository.getTotalSellAmount(userId);
        if (totalSales == null) totalSales = BigDecimal.ZERO;

        BigDecimal totalBuyFees = transactionRepository.getTotalBuyFees(userId);
        if (totalBuyFees == null) totalBuyFees = BigDecimal.ZERO;

        BigDecimal totalSellFees = transactionRepository.getTotalSellFees(userId);
        if (totalSellFees == null) totalSellFees = BigDecimal.ZERO;

        Map<String, BigDecimal> metrics = new HashMap<>();
        metrics.put("totalValue", totalValue);
        metrics.put("totalCost", totalCost);
        metrics.put("totalGain", totalGain);
        metrics.put("percentageReturn", percentageReturn);
        metrics.put("totalInvestment", totalInvestment);
        metrics.put("totalSales", totalSales);
        metrics.put("totalBuyFees", totalBuyFees);
        metrics.put("totalSellFees", totalSellFees);
        metrics.put("totalFees", totalBuyFees.add(totalSellFees));
        metrics.put("realizedGain", totalSales.subtract(totalBuyFees).subtract(totalSellFees));

        return metrics;
    }

    /**
     * Get sector allocation breakdown
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getSectorAllocation(Long userId) {
        List<Map<String, Object>> sectorValues = positionRepository.getSectorAllocation(userId);

        // Calculate total value
        BigDecimal totalValue = sectorValues.stream()
                .map(map -> (Double) map.get("value"))
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate percentages
        Map<String, BigDecimal> result = new HashMap<>();
        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            for (Map<String, Object> entry : sectorValues) {
                String sector = (String) entry.get("sector");
                BigDecimal value = BigDecimal.valueOf((Double) entry.get("value"));
                BigDecimal percentage = value.multiply(BigDecimal.valueOf(100))
                        .divide(totalValue, 2, RoundingMode.HALF_UP);
                result.put(sector, percentage);
            }
        }

        return result;
    }

    /**
     * Get largest positions by value
     */
    @Transactional(readOnly = true)
    public List<Position> getLargestPositions(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return positionRepository.findLargestPositionsByValue(userId, pageable);
    }

    /**
     * Get positions with gains above a certain percentage
     */
    @Transactional(readOnly = true)
    public List<Position> getPositionsWithGainAbove(Long userId, double gainPercentage) {
        BigDecimal gainThreshold = BigDecimal.valueOf(gainPercentage / 100);
        return positionRepository.findPositionsWithGainAbove(userId, gainThreshold);
    }

    /**
     * Get monthly transaction summary
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMonthlyTransactionSummary(Long userId) {
        return transactionRepository.getMonthlyTransactionSummary(userId);
    }
}