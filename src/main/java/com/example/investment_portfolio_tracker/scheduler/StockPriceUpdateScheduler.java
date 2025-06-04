package com.example.investment_portfolio_tracker.scheduler;

import com.example.investment_portfolio_tracker.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class StockPriceUpdateScheduler {

    private final StockService stockService;

    // Run every day at 1:00 AM
    @Scheduled(cron = "0 0 1 * * *")
    public void updateStockPrices() {
        log.info("Starting scheduled stock price update");

        try {
            stockService.refreshAllStockPrices();
            log.info("Completed stock price update");
        } catch (Exception e) {
            log.error("Error during scheduled stock price update: {}", e.getMessage());
        }
    }
}