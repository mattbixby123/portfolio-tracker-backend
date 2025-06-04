package com.example.investment_portfolio_tracker.service;

import com.example.investment_portfolio_tracker.dto.external.AlphaVantageQuote;
import com.example.investment_portfolio_tracker.model.Stock;
import com.example.investment_portfolio_tracker.repository.StockRepository;
import com.example.investment_portfolio_tracker.service.external.AlphaVantageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final AlphaVantageService alphaVantageService;

    // Simple in-memory cache for frequently accessed stocks
    private final Map<String, Stock> stockCache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Stock> getStockById(Long id) {
        return stockRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Stock> getStockByTicker(String ticker) {
        // Try to get from cache first
        Stock cachedStock = stockCache.get(ticker.toUpperCase());
        if (cachedStock != null) {
            return Optional.of(cachedStock);
        }

        // Get from database
        Optional<Stock> stockOpt = stockRepository.findByTickerIgnoreCase(ticker);

        // Update cache if found
        stockOpt.ifPresent(stock -> stockCache.put(ticker.toUpperCase(), stock));

        return stockOpt;
    }

    @Transactional(readOnly = true)
    public List<Stock> searchStocks(String query) {
        return stockRepository.searchByTickerOrName(query);
    }

    @Transactional(readOnly = true)
    public List<Stock> getTopStocksByPrice(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return stockRepository.findTopByPrice(pageable);
    }

    @Transactional(readOnly = true)
    public List<Stock> getStocksNeedingUpdate(int minutesThreshold) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusMinutes(minutesThreshold);
        return stockRepository.findStocksNeedingUpdate(cutoffDate);
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getAveragePriceBySector() {
        return stockRepository.findAll().stream()
                .filter(s -> s.getSector() != null && s.getCurrentPrice() != null)
                .collect(Collectors.groupingBy(
                        Stock::getSector,
                        Collectors.averagingDouble(s -> s.getCurrentPrice().doubleValue())
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> BigDecimal.valueOf(e.getValue())
                ));
    }

    @Transactional
    public Stock createStock(String ticker, String name, String exchange, String currency) {
        if (stockRepository.existsByTickerIgnoreCase(ticker)) {
            throw new IllegalArgumentException("Stock with ticker " + ticker + " already exists");
        }

        Stock stock = new Stock();
        stock.setTicker(ticker.toUpperCase());
        stock.setName(name);
        stock.setExchange(exchange);
        stock.setCurrency(currency);
        stock.setLastUpdated(LocalDateTime.now());

        Stock savedStock = stockRepository.save(stock);
        log.info("Created new stock: {}", ticker);

        // Add to cache
        stockCache.put(ticker.toUpperCase(), savedStock);

        return savedStock;
    }

    @Transactional
    public Stock updateStock(Long id, String name, String exchange, String sector, String industry, String currency) {
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found"));

        stock.setName(name);
        stock.setExchange(exchange);
        stock.setSector(sector);
        stock.setIndustry(industry);
        stock.setCurrency(currency);

        Stock updatedStock = stockRepository.save(stock);
        log.info("Updated stock: {}", stock.getTicker());

        // Update cache
        stockCache.put(stock.getTicker(), updatedStock);

        return updatedStock;
    }

    @Transactional
    public void updateStockPrice(Long id, BigDecimal price) {
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found"));

        stock.setCurrentPrice(price);
        stock.setLastUpdated(LocalDateTime.now());

        Stock updatedStock = stockRepository.save(stock);
        log.info("Updated price for {}: {}", stock.getTicker(), price);

        // Update cache
        stockCache.put(stock.getTicker(), updatedStock);
    }

    @Transactional
    public void updateStockPriceByTicker(String ticker, BigDecimal price) {
        Stock stock = stockRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + ticker));

        stock.setCurrentPrice(price);
        stock.setLastUpdated(LocalDateTime.now());

        Stock updatedStock = stockRepository.save(stock);
        log.info("Updated price for {}: {}", ticker, price);

        // Update cache
        stockCache.put(ticker.toUpperCase(), updatedStock);
    }

    @Transactional
    public void deleteStock(Long id) {
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found"));

        // Remove from cache before deleting
        stockCache.remove(stock.getTicker());

        stockRepository.deleteById(id);
        log.info("Deleted stock: {}", stock.getTicker());
    }

    /**
     * Batch update multiple stock prices
     */
    @Transactional
    public void batchUpdatePrices(List<Stock> stocks) {
        for (Stock stock : stocks) {
            Optional<Stock> existingStock = stockRepository.findByTickerIgnoreCase(stock.getTicker());
            if (existingStock.isPresent()) {
                Stock current = existingStock.get();
                current.setCurrentPrice(stock.getCurrentPrice());
                current.setLastUpdated(LocalDateTime.now());
                Stock updatedStock = stockRepository.save(current);

                // Update cache
                stockCache.put(current.getTicker(), updatedStock);
            }
        }
        log.info("Batch updated prices for {} stocks", stocks.size());
    }

    /**
     * Clear the stock cache to force fresh data on next request
     */
    public void clearCache() {
        stockCache.clear();
        log.info("Stock cache cleared");
    }

    /**
     * Refresh stock price using Alpha Vantage API
     */
    @Transactional
    public Stock refreshStockPrice(String ticker) {
        Stock stock = stockRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + ticker));

        try {
            AlphaVantageQuote quote = alphaVantageService.getQuote(ticker);
            stock.setCurrentPrice(quote.getPrice());

            Stock updatedStock = stockRepository.save(stock);
            log.info("Refreshed price for {} from Alpha Vantage: {}", ticker, quote.getPrice());

            // Update cache
            stockCache.put(ticker.toUpperCase(), updatedStock);

            return updatedStock;
        } catch (Exception e) {
            log.error("Failed to refresh price for {} from Alpha Vantage: {}", ticker, e.getMessage());
            throw new RuntimeException("Unable to refresh stock price from Alpha Vantage", e);
        }
    }

    /**
     * Refresh all stock prices using Alpha Vantage API (respects rate limits)
     */
    @Transactional
    public List<Stock> refreshAllStockPrices() {
        List<Stock> stocks = stockRepository.findAll();
        List<Stock> updatedStocks = new ArrayList<>();

        log.info("Starting refresh for {} stocks from Alpha Vantage", stocks.size());

        for (int i = 0; i < stocks.size(); i++) {
            Stock stock = stocks.get(i);

            try {
                AlphaVantageQuote quote = alphaVantageService.getQuote(stock.getTicker());
                stock.setCurrentPrice(quote.getPrice());
                Stock updatedStock = stockRepository.save(stock);
                updatedStocks.add(updatedStock);

                // Update cache
                stockCache.put(stock.getTicker().toUpperCase(), updatedStock);

                log.info("Refreshed price for {} from Alpha Vantage: {}", stock.getTicker(), quote.getPrice());

                // Respect API rate limits (5 calls per minute for free tier)
                if ((i + 1) % 5 == 0 && i < stocks.size() - 1) {
                    log.info("Pausing for API rate limit...");
                    try {
                        Thread.sleep(60000); // Wait 1 minute after every 5 calls
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Rate limit pause was interrupted");
                    }
                }
            } catch (Exception e) {
                log.error("Failed to refresh price for {} from Alpha Vantage: {}", stock.getTicker(), e.getMessage());
            }
        }

        log.info("Completed refresh for {} stocks from Alpha Vantage", updatedStocks.size());
        return updatedStocks;
    }

    /**
     * Add a new stock by fetching details from Alpha Vantage
     */
    @Transactional
    public Stock addNewStockFromAlphaVantage(String ticker) {
        // Check if stock already exists
        Optional<Stock> existingStock = stockRepository.findByTickerIgnoreCase(ticker);
        if (existingStock.isPresent()) {
            return existingStock.get();
        }

        try {
            // Fetch quote for price
            AlphaVantageQuote quote = alphaVantageService.getQuote(ticker);

            // Fetch company overview for other details
            Map<String, Object> overview = alphaVantageService.getCompanyOverview(ticker);

            // Create new stock
            Stock newStock = new Stock();
            newStock.setTicker(ticker.toUpperCase());
            newStock.setName((String) overview.getOrDefault("Name", ticker.toUpperCase()));
            newStock.setExchange((String) overview.getOrDefault("Exchange", "Unknown"));
            newStock.setSector((String) overview.getOrDefault("Sector", "Unknown"));
            newStock.setIndustry((String) overview.getOrDefault("Industry", "Unknown"));
            newStock.setCurrency((String) overview.getOrDefault("Currency", "USD"));
            newStock.setCurrentPrice(quote.getPrice());
            newStock.setLastUpdated(LocalDateTime.now());

            Stock savedStock = stockRepository.save(newStock);
            log.info("Added new stock from Alpha Vantage: {}", ticker);

            // Add to cache
            stockCache.put(ticker.toUpperCase(), savedStock);

            return savedStock;
        } catch (Exception e) {
            log.error("Failed to add new stock {} from Alpha Vantage: {}", ticker, e.getMessage());
            throw new RuntimeException("Unable to add stock from Alpha Vantage", e);
        }
    }
}