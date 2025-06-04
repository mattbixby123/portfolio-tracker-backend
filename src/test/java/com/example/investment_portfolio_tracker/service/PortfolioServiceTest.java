package com.example.investment_portfolio_tracker.service;

import com.example.investment_portfolio_tracker.model.*;
import com.example.investment_portfolio_tracker.repository.PositionRepository;
import com.example.investment_portfolio_tracker.repository.StockRepository;
import com.example.investment_portfolio_tracker.repository.TransactionRepository;
import com.example.investment_portfolio_tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private PortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        portfolioService = new PortfolioService(
                userRepository,
                stockRepository,
                positionRepository,
                transactionRepository
        );
    }

    @Test
    void shouldGetUserPositions() {
        // Given
        Position position1 = new Position();
        position1.setId(1L);
        Position position2 = new Position();
        position2.setId(2L);

        when(positionRepository.findByUserId(1L)).thenReturn(Arrays.asList(position1, position2));

        // When
        List<Position> positions = portfolioService.getUserPositions(1L);

        // Then
        assertThat(positions).hasSize(2);
        assertThat(positions.get(0).getId()).isEqualTo(1L);
        assertThat(positions.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void shouldGetPosition() {
        // Given
        Position position = new Position();
        position.setId(1L);

        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));

        // When
        Optional<Position> result = portfolioService.getPosition(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void shouldGetPortfolioValue() {
        // Given
        when(positionRepository.getTotalPortfolioValue(1L)).thenReturn(5000.0);

        // When
        BigDecimal value = portfolioService.getPortfolioValue(1L);

        // Then
        assertThat(value).isEqualTo(BigDecimal.valueOf(5000.0));
    }

    @Test
    void shouldReturnZeroWhenNoPositions() {
        // Given
        when(positionRepository.getTotalPortfolioValue(1L)).thenReturn(null);

        // When
        BigDecimal value = portfolioService.getPortfolioValue(1L);

        // Then
        assertThat(value).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void shouldGetUserTransactions() {
        // Given
        Transaction transaction1 = new Transaction();
        transaction1.setId(1L);
        Transaction transaction2 = new Transaction();
        transaction2.setId(2L);

        when(transactionRepository.findByUserId(1L)).thenReturn(Arrays.asList(transaction1, transaction2));

        // When
        List<Transaction> transactions = portfolioService.getUserTransactions(1L);

        // Then
        assertThat(transactions).hasSize(2);
    }

    @Test
    void shouldGetStockTransactions() {
        // Given
        Transaction transaction1 = new Transaction();
        transaction1.setId(1L);

        when(transactionRepository.findByUserIdAndStockIdOrderByDateDesc(1L, 2L))
                .thenReturn(List.of(transaction1));

        // When
        List<Transaction> transactions = portfolioService.getStockTransactions(1L, 2L);

        // Then
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void shouldBuyStockForNewPosition() {
        // Given
        User user = new User();
        user.setId(1L);

        Stock stock = new Stock();
        stock.setId(2L);
        stock.setTicker("AAPL");

        LocalDateTime transactionDate = LocalDateTime.now();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(stockRepository.findByTickerIgnoreCase("AAPL")).thenReturn(Optional.of(stock));
        when(positionRepository.findByUserAndStock(user, stock)).thenReturn(Optional.empty());

        Position savedPosition = new Position();
        savedPosition.setId(3L);
        savedPosition.setUser(user);
        savedPosition.setStock(stock);
        when(positionRepository.save(any(Position.class))).thenReturn(savedPosition);

        // Updated mock to return the actual transaction object being saved
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction savedTransaction = invocation.getArgument(0);
            savedTransaction.setId(4L);
            return savedTransaction;
        });

        // When
        Transaction result = portfolioService.buyStock(
                1L,
                "AAPL",
                new BigDecimal("10"),
                new BigDecimal("150.00"),
                new BigDecimal("5.00"),
                transactionDate
        );

        // Then
        assertThat(result.getId()).isEqualTo(4L);

        ArgumentCaptor<Position> positionCaptor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(positionCaptor.capture());

        Position capturedPosition = positionCaptor.getValue();
        assertThat(capturedPosition.getUser()).isEqualTo(user);
        assertThat(capturedPosition.getStock()).isEqualTo(stock);
        assertThat(capturedPosition.getQuantity()).isEqualTo(new BigDecimal("10"));
        assertThat(capturedPosition.getAverageCost()).isEqualTo(new BigDecimal("150.00"));
        assertThat(capturedPosition.getFirstPurchased()).isEqualTo(transactionDate);
        assertThat(capturedPosition.getLastTransaction()).isEqualTo(transactionDate);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        Transaction capturedTransaction = transactionCaptor.getValue();
        assertThat(capturedTransaction.getUser()).isEqualTo(user);
        assertThat(capturedTransaction.getStock()).isEqualTo(stock);
        assertThat(capturedTransaction.getPosition()).isEqualTo(savedPosition);
        assertThat(capturedTransaction.getTransactionType()).isEqualTo(TransactionType.BUY);
        assertThat(capturedTransaction.getQuantity()).isEqualTo(new BigDecimal("10"));
        assertThat(capturedTransaction.getPrice()).isEqualTo(new BigDecimal("150.00"));
        assertThat(capturedTransaction.getFee()).isEqualTo(new BigDecimal("5.00"));
        assertThat(capturedTransaction.getTransactionDate()).isEqualTo(transactionDate);
    }

    @Test
    void shouldBuyStockForExistingPosition() {
        // Given
        User user = new User();
        user.setId(1L);

        Stock stock = new Stock();
        stock.setId(2L);
        stock.setTicker("AAPL");

        Position existingPosition = new Position();
        existingPosition.setId(3L);
        existingPosition.setUser(user);
        existingPosition.setStock(stock);
        existingPosition.setQuantity(new BigDecimal("10"));
        existingPosition.setAverageCost(new BigDecimal("140.00"));
        existingPosition.setFirstPurchased(LocalDateTime.now().minusDays(10));

        LocalDateTime transactionDate = LocalDateTime.now();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(stockRepository.findByTickerIgnoreCase("AAPL")).thenReturn(Optional.of(stock));
        when(positionRepository.findByUserAndStock(user, stock)).thenReturn(Optional.of(existingPosition));
        when(positionRepository.save(any(Position.class))).thenReturn(existingPosition);

        // Updated mock to return the actual transaction object being saved
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction savedTransaction = invocation.getArgument(0);
            savedTransaction.setId(4L);
            return savedTransaction;
        });

        // When
        Transaction result = portfolioService.buyStock(
                1L,
                "AAPL",
                new BigDecimal("5"),
                new BigDecimal("160.00"),
                new BigDecimal("5.00"),
                transactionDate
        );

        // Then
        assertThat(result.getId()).isEqualTo(4L);

        // Check position updates
        assertThat(existingPosition.getQuantity()).isEqualTo(new BigDecimal("15")); // 10 + 5
        // Average cost should be (10*140 + 5*160) / 15 = 146.67
        assertThat(existingPosition.getAverageCost().doubleValue()).isEqualTo(146.67, within(0.01));
        assertThat(existingPosition.getLastTransaction()).isEqualTo(transactionDate);

        verify(positionRepository).save(existingPosition);

        // Check transaction creation
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        Transaction capturedTransaction = transactionCaptor.getValue();
        assertThat(capturedTransaction.getUser()).isEqualTo(user);
        assertThat(capturedTransaction.getStock()).isEqualTo(stock);
        assertThat(capturedTransaction.getPosition()).isEqualTo(existingPosition);
        assertThat(capturedTransaction.getTransactionType()).isEqualTo(TransactionType.BUY);
        assertThat(capturedTransaction.getQuantity()).isEqualTo(new BigDecimal("5"));
        assertThat(capturedTransaction.getPrice()).isEqualTo(new BigDecimal("160.00"));
    }

    @Test
    void shouldSellStock() {
        // Given
        User user = new User();
        user.setId(1L);

        Stock stock = new Stock();
        stock.setId(2L);
        stock.setTicker("AAPL");

        Position existingPosition = new Position();
        existingPosition.setId(3L);
        existingPosition.setUser(user);
        existingPosition.setStock(stock);
        existingPosition.setQuantity(new BigDecimal("10"));
        existingPosition.setAverageCost(new BigDecimal("140.00"));

        LocalDateTime transactionDate = LocalDateTime.now();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(stockRepository.findByTickerIgnoreCase("AAPL")).thenReturn(Optional.of(stock));
        when(positionRepository.findByUserAndStock(user, stock)).thenReturn(Optional.of(existingPosition));
        when(positionRepository.save(any(Position.class))).thenReturn(existingPosition);

        // Updated mock to return the actual transaction object being saved
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction savedTransaction = invocation.getArgument(0);
            savedTransaction.setId(4L);
            return savedTransaction;
        });

        // When
        Transaction result = portfolioService.sellStock(
                1L,
                "AAPL",
                new BigDecimal("5"),
                new BigDecimal("160.00"),
                new BigDecimal("5.00"),
                transactionDate
        );

        // Then
        assertThat(result.getId()).isEqualTo(4L);

        // Check position updates
        assertThat(existingPosition.getQuantity()).isEqualTo(new BigDecimal("5")); // 10 - 5
        assertThat(existingPosition.getLastTransaction()).isEqualTo(transactionDate);
        // Average cost should remain the same
        assertThat(existingPosition.getAverageCost()).isEqualTo(new BigDecimal("140.00"));

        verify(positionRepository).save(existingPosition);

        // Check transaction creation
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        Transaction capturedTransaction = transactionCaptor.getValue();
        assertThat(capturedTransaction.getTransactionType()).isEqualTo(TransactionType.SELL);
        assertThat(capturedTransaction.getQuantity()).isEqualTo(new BigDecimal("5"));
        assertThat(capturedTransaction.getPrice()).isEqualTo(new BigDecimal("160.00"));
    }

    @Test
    void shouldNotSellMoreThanOwned() {
        // Given
        User user = new User();
        user.setId(1L);

        Stock stock = new Stock();
        stock.setId(2L);
        stock.setTicker("AAPL");

        Position existingPosition = new Position();
        existingPosition.setId(3L);
        existingPosition.setUser(user);
        existingPosition.setStock(stock);
        existingPosition.setQuantity(new BigDecimal("10"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(stockRepository.findByTickerIgnoreCase("AAPL")).thenReturn(Optional.of(stock));
        when(positionRepository.findByUserAndStock(user, stock)).thenReturn(Optional.of(existingPosition));

        // When/Then
        assertThatThrownBy(() ->
                portfolioService.sellStock(
                        1L,
                        "AAPL",
                        new BigDecimal("15"), // Trying to sell more than owned
                        new BigDecimal("160.00"),
                        new BigDecimal("5.00"),
                        LocalDateTime.now()
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient shares to sell");

        verify(positionRepository, never()).save(any(Position.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldCalculatePerformanceMetrics() {
        // Given
        Position position1 = createPositionWithPerformance(
                1L, new BigDecimal("10"), new BigDecimal("150.00"), new BigDecimal("160.00"));
        Position position2 = createPositionWithPerformance(
                2L, new BigDecimal("5"), new BigDecimal("200.00"), new BigDecimal("180.00"));

        when(positionRepository.findByUserId(1L)).thenReturn(Arrays.asList(position1, position2));
        when(transactionRepository.getTotalInvestmentAmount(1L)).thenReturn(new BigDecimal("2500.00"));
        when(transactionRepository.getTotalSellAmount(1L)).thenReturn(new BigDecimal("1000.00"));
        when(transactionRepository.getTotalBuyFees(1L)).thenReturn(new BigDecimal("25.00"));
        when(transactionRepository.getTotalSellFees(1L)).thenReturn(new BigDecimal("10.00"));

        // When
        Map<String, BigDecimal> metrics = portfolioService.calculatePerformanceMetrics(1L);

        // Then
        assertThat(metrics).containsKey("totalValue");
        assertThat(metrics).containsKey("totalCost");
        assertThat(metrics).containsKey("totalGain");
        assertThat(metrics).containsKey("percentageReturn");
        assertThat(metrics).containsKey("totalInvestment");
        assertThat(metrics).containsKey("totalSales");

        // Expected values:
        // Position 1: 10 shares at $160 = $1600, cost basis 10 * $150 = $1500, gain $100
        // Position 2: 5 shares at $180 = $900, cost basis 5 * $200 = $1000, loss $100
        // Total value: $2500, total cost: $2500, total gain: $0, return: 0%
        assertThat(metrics.get("totalValue")).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(metrics.get("totalCost")).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(metrics.get("totalGain")).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(metrics.get("percentageReturn")).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(metrics.get("totalInvestment")).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(metrics.get("totalSales")).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void shouldGetSectorAllocation() {
        // Given
        when(positionRepository.getSectorAllocation(1L)).thenReturn(Arrays.asList(
                Map.of("sector", "Technology", "value", 5000.0),
                Map.of("sector", "Finance", "value", 3000.0),
                Map.of("sector", "Healthcare", "value", 2000.0)
        ));

        // When
        Map<String, BigDecimal> allocation = portfolioService.getSectorAllocation(1L);

        // Then
        assertThat(allocation).hasSize(3);
        assertThat(allocation.get("Technology")).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(allocation.get("Finance")).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(allocation.get("Healthcare")).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    private Position createPositionWithPerformance(Long id, BigDecimal quantity, BigDecimal cost, BigDecimal currentPrice) {
        Position position = new Position();
        position.setId(id);
        position.setQuantity(quantity);
        position.setAverageCost(cost);

        Stock stock = new Stock();
        stock.setCurrentPrice(currentPrice);
        position.setStock(stock);

        return position;
    }
}