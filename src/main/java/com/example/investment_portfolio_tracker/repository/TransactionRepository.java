package com.example.investment_portfolio_tracker.repository;

import com.example.investment_portfolio_tracker.model.Transaction;
import com.example.investment_portfolio_tracker.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserId(Long userId);

    List<Transaction> findByStockId(Long stockId);

    List<Transaction> findByPositionId(Long positionId);

    List<Transaction> findByUserIdAndTransactionType(Long userId, TransactionType type);

    Page<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.stock.id = :stockId ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdAndStockIdOrderByDateDesc(
            @Param("userId") Long userId,
            @Param("stockId") Long stockId
    );

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId")
    Long countTransactionsByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(CASE WHEN t.transactionType = 'BUY' THEN t.quantity * t.price ELSE 0 END) FROM Transaction t WHERE t.user.id = :userId")
    BigDecimal getTotalInvestmentAmount(@Param("userId") Long userId);

    @Query("SELECT SUM(CASE WHEN t.transactionType = 'SELL' THEN t.quantity * t.price ELSE 0 END) FROM Transaction t WHERE t.user.id = :userId")
    BigDecimal getTotalSellAmount(@Param("userId") Long userId);

    @Query("SELECT SUM(CASE WHEN t.transactionType = 'BUY' THEN t.fee ELSE 0 END) FROM Transaction t WHERE t.user.id = :userId")
    BigDecimal getTotalBuyFees(@Param("userId") Long userId);

    @Query("SELECT SUM(CASE WHEN t.transactionType = 'SELL' THEN t.fee ELSE 0 END) FROM Transaction t WHERE t.user.id = :userId")
    BigDecimal getTotalSellFees(@Param("userId") Long userId);

    @Query("SELECT new map(YEAR(t.transactionDate) as year, " +
            "MONTH(t.transactionDate) as month, " +
            "SUM(CASE WHEN t.transactionType = 'BUY' THEN t.quantity * t.price ELSE 0 END) as buyAmount, " +
            "SUM(CASE WHEN t.transactionType = 'SELL' THEN t.quantity * t.price ELSE 0 END) as sellAmount) " +
            "FROM Transaction t WHERE t.user.id = :userId " +
            "GROUP BY YEAR(t.transactionDate), MONTH(t.transactionDate) " +
            "ORDER BY YEAR(t.transactionDate), MONTH(t.transactionDate)")
    List<Map<String, Object>> getMonthlyTransactionSummary(@Param("userId") Long userId);

}
