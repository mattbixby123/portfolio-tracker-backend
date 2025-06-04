package com.example.investment_portfolio_tracker.controller;

import com.example.investment_portfolio_tracker.dto.StockDto;
import com.example.investment_portfolio_tracker.model.Stock;
import com.example.investment_portfolio_tracker.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping
    public ResponseEntity<List<StockDto>> getAllStocks() {
        List<Stock> stocks = stockService.getAllStocks();
        List<StockDto> stockDtos = stocks.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stockDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockDto> getStockById(@PathVariable Long id) {
        return stockService.getStockById(id)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ticker/{ticker}")
    public ResponseEntity<StockDto> getStockByTicker(@PathVariable String ticker) {
        return stockService.getStockByTicker(ticker)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<StockDto>> searchStocks(@RequestParam String query) {
        List<Stock> stocks = stockService.searchStocks(query);
        List<StockDto> stockDtos = stocks.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stockDtos);
    }

    @GetMapping("/top")
    public ResponseEntity<List<StockDto>> getTopStocks(@RequestParam(defaultValue = "10") int limit) {
        List<Stock> stocks = stockService.getTopStocksByPrice(limit);
        List<StockDto> stockDtos = stocks.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stockDtos);
    }

    @GetMapping("/sectors/average-price")
    public ResponseEntity<Map<String, BigDecimal>> getAveragePriceBySector() {
        return ResponseEntity.ok(stockService.getAveragePriceBySector());
    }

    @PostMapping
    public ResponseEntity<StockDto> createStock(@Valid @RequestBody StockDto stockDto) {
        Stock stock = stockService.createStock(
                stockDto.getTicker(),
                stockDto.getName(),
                stockDto.getExchange(),
                stockDto.getCurrency()
        );
        return new ResponseEntity<>(convertToDto(stock), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StockDto> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody StockDto stockDto) {

        Stock stock = stockService.updateStock(
                id,
                stockDto.getName(),
                stockDto.getExchange(),
                stockDto.getSector(),
                stockDto.getIndustry(),
                stockDto.getCurrency()
        );
        return ResponseEntity.ok(convertToDto(stock));
    }

    @PatchMapping("/{id}/price")
    public ResponseEntity<Void> updateStockPrice(
            @PathVariable Long id,
            @RequestParam BigDecimal price) {

        stockService.updateStockPrice(id, price);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/ticker/{ticker}/price")
    public ResponseEntity<Void> updateStockPriceByTicker(
            @PathVariable String ticker,
            @RequestParam BigDecimal price) {

        stockService.updateStockPriceByTicker(ticker, price);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        stockService.deleteStock(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cache/clear")
    public ResponseEntity<Void> clearCache() {
        stockService.clearCache();
        return ResponseEntity.ok().build();
    }

    // New endpoints for Alpha Vantage integration

    @PutMapping("/ticker/{ticker}/refresh")
    public ResponseEntity<StockDto> refreshStockPrice(@PathVariable String ticker) {
        Stock refreshedStock = stockService.refreshStockPrice(ticker);
        return ResponseEntity.ok(convertToDto(refreshedStock));
    }

    @PutMapping("/refresh-all")
    public ResponseEntity<String> refreshAllStocks() {
        stockService.refreshAllStockPrices();
        return ResponseEntity.ok("Stock price update initiated");
    }

    @PostMapping("/lookup/{ticker}")
    public ResponseEntity<StockDto> addStockFromAlphaVantage(@PathVariable String ticker) {
        Stock newStock = stockService.addNewStockFromAlphaVantage(ticker);
        return new ResponseEntity<>(convertToDto(newStock), HttpStatus.CREATED);
    }

    // Helper method to convert Stock to StockDto
    private StockDto convertToDto(Stock stock) {
        return StockDto.builder()
                .id(stock.getId())
                .ticker(stock.getTicker())
                .name(stock.getName())
                .exchange(stock.getExchange())
                .sector(stock.getSector())
                .industry(stock.getIndustry())
                .currency(stock.getCurrency())
                .currentPrice(stock.getCurrentPrice())
                .lastUpdated(stock.getLastUpdated())
                .build();
    }

    // Helper method to convert StockDto to Stock (if needed)
    private Stock convertToEntity(StockDto stockDto) {
        Stock stock = new Stock();
        stock.setId(stockDto.getId());
        stock.setTicker(stockDto.getTicker());
        stock.setName(stockDto.getName());
        stock.setExchange(stockDto.getExchange());
        stock.setSector(stockDto.getSector());
        stock.setIndustry(stockDto.getIndustry());
        stock.setCurrency(stockDto.getCurrency());
        stock.setCurrentPrice(stockDto.getCurrentPrice());
        stock.setLastUpdated(stockDto.getLastUpdated());
        return stock;
    }
}