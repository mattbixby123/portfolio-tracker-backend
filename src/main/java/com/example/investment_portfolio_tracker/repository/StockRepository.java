package com.example.investment_portfolio_tracker.repository;

import com.example.investment_portfolio_tracker.model.Stock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByTickerIgnoreCase(String ticker);

    boolean existsByTickerIgnoreCase(String ticker);

    List<Stock> findByExchangeIgnoreCase(String exchange);

    List<Stock> findBySectorIgnoreCase(String sector);

    List<Stock> findByIndustryIgnoreCase(String industry);

    @Query("SELECT s FROM Stock s WHERE LOWER(s.ticker) LIKE LOWER (CONCAT('%', :query, '%')) OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Stock> searchByTickerOrName(@Param("query") String query);

    @Query("SELECT s FROM Stock s WHERE s.currentPrice IS NOT NULL ORDER BY s.currentPrice DESC")
    List<Stock> findTopByPrice(Pageable pageable);

    @Query("SELECT s FROM Stock s WHERE s.lastUpdated > :cutoffDate")
    List<Stock> findStocksNeedingUpdate(@Param("cutoffDate")LocalDateTime cutoffDate);

    @Query("SELECT AVG(s.currentPrice) FROM Stock s WHERE s.sector = :sector")
    BigDecimal getAveragePriceBySector(@Param("sector") String sector);
}
