package com.example.investment_portfolio_tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PositionDto {
    private Long id;
    private Long stockId;
    private String stockTicker;
    private String stockName;
    private BigDecimal quantity;
    private BigDecimal averageCost;
    private BigDecimal currentPrice;
    private LocalDateTime firstPurchased;
    private LocalDateTime lastTransaction;
    private BigDecimal currentValue;
    private BigDecimal totalCost;
    private BigDecimal unrealizedProfitLoss;
    private BigDecimal percentageReturn;
    private String notes;
}