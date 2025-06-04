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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PortfolioService portfolioService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<PositionDto>> getUserPositions(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Position> positions = portfolioService.getUserPositions(user.getId());
        List<PositionDto> positionDtos = positions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(positionDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PositionDto> getPositionById(@PathVariable Long id, Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Position position = portfolioService.getPosition(id)
                .orElseThrow(() -> new IllegalArgumentException("Position not found"));

        // Verify this position belongs to the authenticated user
        if (!position.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(convertToDto(position));
    }

    @GetMapping("/value")
    public ResponseEntity<Map<String, BigDecimal>> getPortfolioValue(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        BigDecimal value = portfolioService.getPortfolioValue(user.getId());
        return ResponseEntity.ok(Map.of("totalValue", value));
    }

    @GetMapping("/largest")
    public ResponseEntity<List<PositionDto>> getLargestPositions(
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

    @GetMapping("/gains")
    public ResponseEntity<List<PositionDto>> getPositionsWithGains(
            @RequestParam(defaultValue = "10") double gainPercentage,
            Authentication authentication) {

        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Position> positions = portfolioService.getPositionsWithGainAbove(user.getId(), gainPercentage);
        List<PositionDto> positionDtos = positions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(positionDtos);
    }

    @GetMapping("/sector-allocation")
    public ResponseEntity<Map<String, BigDecimal>> getSectorAllocation(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Map<String, BigDecimal> sectorAllocation = portfolioService.getSectorAllocation(user.getId());
        return ResponseEntity.ok(sectorAllocation);
    }

    // Helper method to convert Position to PositionDto
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