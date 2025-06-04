package com.example.investment_portfolio_tracker.controller;

import com.example.investment_portfolio_tracker.dto.PositionDto;
import com.example.investment_portfolio_tracker.model.Position;
import com.example.investment_portfolio_tracker.model.User;
import com.example.investment_portfolio_tracker.service.PortfolioService;
import com.example.investment_portfolio_tracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UserService userService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getPortfolioSummary(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get performance metrics
        Map<String, BigDecimal> metrics = portfolioService.calculatePerformanceMetrics(user.getId());

        // Get portfolio value
        BigDecimal portfolioValue = portfolioService.getPortfolioValue(user.getId());

        // Get position count
        List<Position> positions = portfolioService.getUserPositions(user.getId());

        // Prepare summary map
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPositions", positions.size());
        summary.put("totalValue", portfolioValue);
        summary.putAll(metrics);

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/performance")
    public ResponseEntity<Map<String, BigDecimal>> getPortfolioPerformance(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Map<String, BigDecimal> metrics = portfolioService.calculatePerformanceMetrics(user.getId());
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/top-holdings")
    public ResponseEntity<List<PositionDto>> getTopHoldings(
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {

        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Position> positions = portfolioService.getLargestPositions(user.getId(), limit);
        List<PositionDto> positionDtos = positions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(positionDtos);
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyTransactionSummary(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Map<String, Object>> monthlySummary = portfolioService.getMonthlyTransactionSummary(user.getId());
        return ResponseEntity.ok(monthlySummary);
    }

    @GetMapping("/allocation")
    public ResponseEntity<Map<String, BigDecimal>> getSectorAllocation(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Map<String, BigDecimal> sectorAllocation = portfolioService.getSectorAllocation(user.getId());
        return ResponseEntity.ok(sectorAllocation);
    }

    // Helper method to convert Position to PositionDto (same as in PositionController)
    private PositionDto convertToDto(Position position) {
        BigDecimal currentPrice = position.getStock().getCurrentPrice();
        BigDecimal quantity = position.getQuantity();
        BigDecimal averageCost = position.getAverageCost();

        // Calculate derived values
        BigDecimal currentValue = currentPrice != null && quantity != null
                ? currentPrice.multiply(quantity)
                : null;

        BigDecimal totalCost = quantity != null && averageCost != null
                ? quantity.multiply(averageCost)
                : null;

        BigDecimal unrealizedProfitLoss = currentValue != null && totalCost != null
                ? currentValue.subtract(totalCost)
                : null;

        BigDecimal percentageReturn = null;
        if (totalCost != null && totalCost.compareTo(BigDecimal.ZERO) > 0 && unrealizedProfitLoss != null) {
            percentageReturn = unrealizedProfitLoss.multiply(BigDecimal.valueOf(100))
                    .divide(totalCost, 2, BigDecimal.ROUND_HALF_UP);
        }

        return PositionDto.builder()
                .id(position.getId())
                .stockId(position.getStock().getId())
                .stockTicker(position.getStock().getTicker())
                .stockName(position.getStock().getName())
                .quantity(position.getQuantity())
                .averageCost(position.getAverageCost())
                .currentPrice(currentPrice)
                .firstPurchased(position.getFirstPurchased())
                .lastTransaction(position.getLastTransaction())
                .currentValue(currentValue)
                .totalCost(totalCost)
                .unrealizedProfitLoss(unrealizedProfitLoss)
                .percentageReturn(percentageReturn)
                .notes(position.getNotes())
                .build();
    }
}