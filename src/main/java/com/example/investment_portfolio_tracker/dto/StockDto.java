package com.example.investment_portfolio_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class StockDto {

    private Long id;

    @NotBlank(message = "Ticker is required")
    @Size(max = 20, message = "Ticker must be 20 characters or less")
    @Pattern(regexp = "^[A-Z0-9.]+$", message = "Ticker must consist of uppercase letters, numbers, or dots")
    private String ticker;

    @NotBlank(message = "Stock name is required")
    @Size(max = 255, message = "Stock name must be 255 characters or less")
    private String name;

    @NotBlank(message = "Exchange is required")
    private String exchange;

    private String sector;

    private String industry;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    private String currency;

    private BigDecimal currentPrice;

    private LocalDateTime lastUpdated;
}