package com.example.investment_portfolio_tracker.service;

import com.example.investment_portfolio_tracker.model.Stock;
import com.example.investment_portfolio_tracker.repository.StockRepository;
import com.example.investment_portfolio_tracker.service.external.AlphaVantageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private AlphaVantageService alphaVantageService;  // Add this line

    private StockService stockService;

    @BeforeEach
    void setUp() {
        stockService = new StockService(stockRepository, alphaVantageService);
    }

    @Test
    void shouldGetAllStocks() {
        // Given
        Stock stock1 = new Stock();
        stock1.setId(1L);
        stock1.setTicker("AAPL");

        Stock stock2 = new Stock();
        stock2.setId(2L);
        stock2.setTicker("MSFT");

        when(stockRepository.findAll()).thenReturn(Arrays.asList(stock1, stock2));

        // When
        List<Stock> stocks = stockService.getAllStocks();

        // Then
        assertThat(stocks).hasSize(2);
        assertThat(stocks.get(0).getTicker()).isEqualTo("AAPL");
        assertThat(stocks.get(1).getTicker()).isEqualTo("MSFT");
    }

    @Test
    void shouldGetStockById() {
        // Given
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setTicker("AAPL");

        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock));

        // When
        Optional<Stock> result = stockService.getStockById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTicker()).isEqualTo("AAPL");
    }

    @Test
    void shouldGetStockByTicker() {
        // Given
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setTicker("AAPL");

        when(stockRepository.findByTickerIgnoreCase("AAPL")).thenReturn(Optional.of(stock));

        // When
        Optional<Stock> result = stockService.getStockByTicker("AAPL");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void shouldGetStockByTickerFromCache() {
        // Given
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setTicker("AAPL");

        // First call will hit the repository
        when(stockRepository.findByTickerIgnoreCase("AAPL")).thenReturn(Optional.of(stock));

        // First call
        stockService.getStockByTicker("AAPL");

        // Second call should use cache
        Optional<Stock> result = stockService.getStockByTicker("AAPL");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);

        // Verify repository was only called once
        verify(stockRepository, times(1)).findByTickerIgnoreCase("AAPL");
    }

    @Test
    void shouldSearchStocks() {
        // Given
        Stock stock1 = new Stock();
        stock1.setTicker("AAPL");
        stock1.setName("Apple Inc.");

        Stock stock2 = new Stock();
        stock2.setTicker("AMZN");
        stock2.setName("Amazon.com Inc.");

        when(stockRepository.searchByTickerOrName("A")).thenReturn(Arrays.asList(stock1, stock2));

        // When
        List<Stock> results = stockService.searchStocks("A");

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTicker()).isEqualTo("AAPL");
        assertThat(results.get(1).getTicker()).isEqualTo("AMZN");
    }

    @Test
    void shouldGetTopStocksByPrice() {
        // Given
        Stock stock1 = new Stock();
        stock1.setTicker("GOOGL");
        stock1.setCurrentPrice(new BigDecimal("2500.00"));

        Stock stock2 = new Stock();
        stock2.setTicker("AMZN");
        stock2.setCurrentPrice(new BigDecimal("3500.00"));

        Pageable pageable = PageRequest.of(0, 2);
        when(stockRepository.findTopByPrice(pageable)).thenReturn(Arrays.asList(stock2, stock1));

        // When
        List<Stock> results = stockService.getTopStocksByPrice(2);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTicker()).isEqualTo("AMZN");
        assertThat(results.get(1).getTicker()).isEqualTo("GOOGL");
    }

    @Test
    void shouldGetStocksNeedingUpdate() {
        // Given
        Stock stock1 = new Stock();
        stock1.setTicker("AAPL");
        stock1.setLastUpdated(LocalDateTime.now().minusHours(2));

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        when(stockRepository.findStocksNeedingUpdate(any(LocalDateTime.class))).thenReturn(List.of(stock1));

        // When
        List<Stock> results = stockService.getStocksNeedingUpdate(30);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTicker()).isEqualTo("AAPL");
    }

    @Test
    void shouldCreateStock() {
        // Given
        when(stockRepository.existsByTickerIgnoreCase(anyString())).thenReturn(false);

        Stock savedStock = new Stock();
        savedStock.setId(1L);
        savedStock.setTicker("AAPL");
        savedStock.setName("Apple Inc.");
        savedStock.setExchange("NASDAQ");
        savedStock.setCurrency("USD");

        when(stockRepository.save(any(Stock.class))).thenReturn(savedStock);

        // When
        Stock createdStock = stockService.createStock(
                "AAPL",
                "Apple Inc.",
                "NASDAQ",
                "USD"
        );

        // Then
        assertThat(createdStock.getId()).isEqualTo(1L);

        ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(stockCaptor.capture());

        Stock capturedStock = stockCaptor.getValue();
        assertThat(capturedStock.getTicker()).isEqualTo("AAPL");
        assertThat(capturedStock.getName()).isEqualTo("Apple Inc.");
        assertThat(capturedStock.getExchange()).isEqualTo("NASDAQ");
        assertThat(capturedStock.getCurrency()).isEqualTo("USD");
        assertThat(capturedStock.getLastUpdated()).isNotNull();
    }

    @Test
    void shouldThrowExceptionWhenTickerExistsOnCreate() {
        // Given
        when(stockRepository.existsByTickerIgnoreCase("AAPL")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() ->
                stockService.createStock(
                        "AAPL",
                        "Apple Inc.",
                        "NASDAQ",
                        "USD"
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock with ticker AAPL already exists");

        verify(stockRepository, never()).save(any(Stock.class));
    }

    @Test
    void shouldUpdateStock() {
        // Given
        Stock existingStock = new Stock();
        existingStock.setId(1L);
        existingStock.setTicker("AAPL");
        existingStock.setName("Apple Inc.");
        existingStock.setExchange("NASDAQ");
        existingStock.setSector("Technology");
        existingStock.setIndustry("Consumer Electronics");
        existingStock.setCurrency("USD");

        when(stockRepository.findById(1L)).thenReturn(Optional.of(existingStock));
        when(stockRepository.save(any(Stock.class))).thenReturn(existingStock);

        // When
        Stock updatedStock = stockService.updateStock(
                1L,
                "Apple Incorporated",
                "NYSE",
                "Tech",
                "Electronics",
                "EUR"
        );

        // Then
        verify(stockRepository).save(existingStock);
        assertThat(updatedStock.getName()).isEqualTo("Apple Incorporated");
        assertThat(updatedStock.getExchange()).isEqualTo("NYSE");
        assertThat(updatedStock.getSector()).isEqualTo("Tech");
        assertThat(updatedStock.getIndustry()).isEqualTo("Electronics");
        assertThat(updatedStock.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void shouldUpdateStockPrice() {
        // Given
        Stock existingStock = new Stock();
        existingStock.setId(1L);
        existingStock.setTicker("AAPL");
        existingStock.setCurrentPrice(new BigDecimal("150.00"));
        existingStock.setLastUpdated(LocalDateTime.now().minusDays(1));

        when(stockRepository.findById(1L)).thenReturn(Optional.of(existingStock));
        when(stockRepository.save(any(Stock.class))).thenReturn(existingStock);

        // When
        stockService.updateStockPrice(1L, new BigDecimal("160.00"));

        // Then
        verify(stockRepository).save(existingStock);
        assertThat(existingStock.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("160.00"));
        assertThat(existingStock.getLastUpdated()).isAfter(LocalDateTime.now().minusMinutes(1));
    }

    @Test
    void shouldClearCache() {
        // Given
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setTicker("AAPL");

        when(stockRepository.findByTickerIgnoreCase("AAPL")).thenReturn(Optional.of(stock));

        // Cache the stock
        stockService.getStockByTicker("AAPL");

        // When
        stockService.clearCache();

        // Then call again - should hit repository
        stockService.getStockByTicker("AAPL");

        // Verify repository was called twice
        verify(stockRepository, times(2)).findByTickerIgnoreCase("AAPL");
    }
}