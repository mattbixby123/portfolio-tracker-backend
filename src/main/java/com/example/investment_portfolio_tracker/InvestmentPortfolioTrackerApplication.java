package com.example.investment_portfolio_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InvestmentPortfolioTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvestmentPortfolioTrackerApplication.class, args);
	}

}
