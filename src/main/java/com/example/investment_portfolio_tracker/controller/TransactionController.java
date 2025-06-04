package com.example.investment_portfolio_tracker.controller;

import com.example.investment_portfolio_tracker.dto.TransactionDto;
import com.example.investment_portfolio_tracker.model.Transaction;
import com.example.investment_portfolio_tracker.model.TransactionType;
import com.example.investment_portfolio_tracker.model.User;
import com.example.investment_portfolio_tracker.service.PortfolioService;
import com.example.investment_portfolio_tracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final PortfolioService portfolioService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<TransactionDto>> getUserTransactions(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Transaction> transactions = portfolioService.getUserTransactions(user.getId());
        List<TransactionDto> transactionDtos = transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(transactionDtos);
    }

    @GetMapping("/paged")
    public ResponseEntity<List<TransactionDto>> getPaginatedTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Transaction> transactions = portfolioService.getPaginatedTransactions(user.getId(), page, size);
        List<TransactionDto> transactionDtos = transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(transactionDtos);
    }

    @GetMapping("/stock/{stockId}")
    public ResponseEntity<List<TransactionDto>> getStockTransactions(
            @PathVariable Long stockId,
            Authentication authentication) {

        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Transaction> transactions = portfolioService.getStockTransactions(user.getId(), stockId);
        List<TransactionDto> transactionDtos = transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(transactionDtos);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<TransactionDto>> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {

        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Transaction> transactions = portfolioService.getTransactionsInDateRange(user.getId(), startDate, endDate);
        List<TransactionDto> transactionDtos = transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(transactionDtos);
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyTransactionSummary(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Map<String, Object>> monthlySummary = portfolioService.getMonthlyTransactionSummary(user.getId());
        return ResponseEntity.ok(monthlySummary);
    }

    @PostMapping("/buy")
    public ResponseEntity<TransactionDto> buyStock(
            @Valid @RequestBody TransactionDto transactionDto,
            Authentication authentication) {

        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Make sure the transaction type is BUY
        if (!"BUY".equals(transactionDto.getTransactionType())) {
            return ResponseEntity.badRequest().build();
        }

        // Default fee to 0 if not provided
        BigDecimal fee = transactionDto.getFee() != null ? transactionDto.getFee() : BigDecimal.ZERO;

        // Default transaction date to now if not provided
        LocalDateTime transactionDate = transactionDto.getTransactionDate() != null ?
                transactionDto.getTransactionDate() : LocalDateTime.now();

        Transaction transaction = portfolioService.buyStock(
                user.getId(),
                transactionDto.getStockTicker(),
                transactionDto.getQuantity(),
                transactionDto.getPrice(),
                fee,
                transactionDate
        );

        return new ResponseEntity<>(convertToDto(transaction), HttpStatus.CREATED);
    }

    @PostMapping("/sell")
    public ResponseEntity<TransactionDto> sellStock(
            @Valid @RequestBody TransactionDto transactionDto,
            Authentication authentication) {

        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Make sure the transaction type is SELL
        if (!"SELL".equals(transactionDto.getTransactionType())) {
            return ResponseEntity.badRequest().build();
        }

        // Default fee to 0 if not provided
        BigDecimal fee = transactionDto.getFee() != null ? transactionDto.getFee() : BigDecimal.ZERO;

        // Default transaction date to now if not provided
        LocalDateTime transactionDate = transactionDto.getTransactionDate() != null ?
                transactionDto.getTransactionDate() : LocalDateTime.now();

        Transaction transaction = portfolioService.sellStock(
                user.getId(),
                transactionDto.getStockTicker(),
                transactionDto.getQuantity(),
                transactionDto.getPrice(),
                fee,
                transactionDate
        );

        return new ResponseEntity<>(convertToDto(transaction), HttpStatus.CREATED);
    }

    // Helper method to convert Transaction to TransactionDto
    private TransactionDto convertToDto(Transaction transaction) {
        BigDecimal value = transaction.getQuantity().multiply(transaction.getPrice());
        BigDecimal totalCost = value;

        if (transaction.getFee() != null) {
            totalCost = transaction.getTransactionType() == TransactionType.BUY ?
                    value.add(transaction.getFee()) : value.subtract(transaction.getFee());
        }

        return TransactionDto.builder()
                .id(transaction.getId())
                .stockId(transaction.getStock().getId())
                .stockTicker(transaction.getStock().getTicker())
                .stockName(transaction.getStock().getName())
                .transactionType(transaction.getTransactionType().toString())
                .quantity(transaction.getQuantity())
                .price(transaction.getPrice())
                .fee(transaction.getFee())
                .value(value)
                .totalCost(totalCost)
                .transactionDate(transaction.getTransactionDate())
                .build();
    }
}