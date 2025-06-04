package com.example.investment_portfolio_tracker.repository;

import com.example.investment_portfolio_tracker.model.Position;
import com.example.investment_portfolio_tracker.model.Stock;
import com.example.investment_portfolio_tracker.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByUser(User user);

    Optional<Position> findByUserAndStock(User user, Stock stock);

    @Query("SELECT p FROM Position p WHERE p.user.id = :userId")
    List<Position> findByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM Position p WHERE p.user.id = :userId AND p.stock.id = :stockId")
    Optional<Position> findByUserIdAndStockId(@Param("userId") Long userId, @Param("stockId") Long stockId);

    @Query("SELECT SUM(p.quantity * s.currentPrice) FROM Position p JOIN p.stock s WHERE p.user.id = :userId")
    Double getTotalPortfolioValue(@Param("userId") Long userId);

    @Query("SELECT COUNT(p) FROM Position p WHERE p.user.id = :userId")
    Long countPositionsByUserId(@Param("userId") Long userId);

    @Query("SELECT new map(s.sector as sector, SUM(p.quantity * s.currentPrice) as value) " +
            "FROM Position p JOIN p.stock s " +
            "WHERE p.user.id = :userId AND s.sector IS NOT NULL " +
            "GROUP BY s.sector")
    List<Map<String, Object>> getSectorAllocation(@Param("userId") Long userId);

    @Query("SELECT p FROM Position p WHERE p.user.id = :userId AND p.quantity > 0 ORDER BY (p.quantity * p.stock.currentPrice) DESC")
    List<Position> findLargestPositionsByValue(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM Position p WHERE p.user.id = :userId AND p.stock.currentPrice / p.averageCost - 1 > :gainPercent")
    List<Position> findPositionsWithGainAbove(@Param("userId") Long userId, @Param("gainPercent") BigDecimal gainPercent);
}
