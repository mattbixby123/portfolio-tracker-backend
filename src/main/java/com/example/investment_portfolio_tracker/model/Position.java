package com.example.investment_portfolio_tracker.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "positions")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "average_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal averageCost;

    @Column(name = "first_purchased", nullable = false)
    private LocalDateTime firstPurchased;

    @Column(name = "last_transaction", nullable = false)
    private LocalDateTime lastTransaction;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "position", cascade = CascadeType.ALL)
    private List<Transaction> transactions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculates the current value of this position
     * @return The current market value
     */
    @Transient
    public BigDecimal getCurrentValue() {
        if (stock.getCurrentPrice() == null) {
            return null;
        }
        return stock.getCurrentPrice().multiply(quantity);
    }

    /**
     * Calculates the total cost basis of this position
     * @return The total cost
     */
    @Transient
    public BigDecimal getTotalCost() {
        return averageCost.multiply(quantity);
    }

    /**
     * Calculates the unrealized gain/loss
     * @return Profit or loss amount
     */
    @Transient
    public BigDecimal getUnrealizedProfitLoss() {
        BigDecimal currentValue = getCurrentValue();
        if (currentValue == null) {
            return null;
        }
        return currentValue.subtract(getTotalCost());
    }

    /**
     * Calculates the percentage return
     * @return Percentage gain/loss
     */
    @Transient
    public BigDecimal getPercentageReturn() {
        BigDecimal unrealizedPL = getUnrealizedProfitLoss();
        BigDecimal totalCost = getTotalCost();

        if (unrealizedPL == null || totalCost.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return unrealizedPL.divide(totalCost, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}