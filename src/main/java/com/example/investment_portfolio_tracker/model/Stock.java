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
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String ticker;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String exchange;

    private String sector;

    private String industry;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "current_price", precision = 19, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @OneToMany(mappedBy = "stock")
    private List<Position> positions = new ArrayList<>();

    @OneToMany(mappedBy = "stock")
    private List<Transaction> transactions = new ArrayList<>();

    @PrePersist
    @PreUpdate
    protected void setLastUpdated() {
        lastUpdated = LocalDateTime.now();
    }
}