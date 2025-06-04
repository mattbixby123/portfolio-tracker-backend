package com.example.investment_portfolio_tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PerformanceDto {
    private BigDecimal totalValue;
    private BigDecimal totalCost;
    private BigDecimal totalGain;
    private BigDecimal percentageReturn;
    private BigDecimal totalInvestment;
    private BigDecimal totalSales;
    private BigDecimal totalBuyFees;
    private BigDecimal totalSellFees;
    private BigDecimal totalFees;
    private BigDecimal realizedGain;
}