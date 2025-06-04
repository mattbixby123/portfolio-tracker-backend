package com.example.investment_portfolio_tracker.service.external;

import com.example.investment_portfolio_tracker.dto.external.AlphaVantageQuote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
public class AlphaVantageService {

    private final RestTemplate restTemplate;
    private final String apiKey;

    public AlphaVantageService(
            @Value("${alphavantage.api-key}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
    }

    public AlphaVantageQuote getQuote(String ticker) {
        String url = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol={symbol}&apikey={apiKey}";

        try {
            Map<String, Object> response = restTemplate.getForObject(
                    url,
                    Map.class,
                    ticker,
                    apiKey
            );

            if (response == null || !response.containsKey("Global Quote")) {
                log.error("Invalid response from Alpha Vantage for ticker {}: {}", ticker, response);
                throw new RuntimeException("Invalid response from Alpha Vantage");
            }

            @SuppressWarnings("unchecked")
            Map<String, String> quoteData = (Map<String, String>) response.get("Global Quote");
            return mapToQuote(quoteData);
        } catch (Exception e) {
            log.error("Error fetching stock quote for {}: {}", ticker, e.getMessage());
            throw new RuntimeException("Failed to fetch stock data", e);
        }
    }

    private AlphaVantageQuote mapToQuote(Map<String, String> quoteData) {
        AlphaVantageQuote quote = new AlphaVantageQuote();
        quote.setSymbol(quoteData.get("01. symbol"));
        quote.setPrice(new BigDecimal(quoteData.get("05. price")));
        quote.setOpen(new BigDecimal(quoteData.get("02. open")));
        quote.setHigh(new BigDecimal(quoteData.get("03. high")));
        quote.setLow(new BigDecimal(quoteData.get("04. low")));
        quote.setVolume(Long.parseLong(quoteData.get("06. volume")));
        quote.setPreviousClose(new BigDecimal(quoteData.get("08. previous close")));
        quote.setChange(new BigDecimal(quoteData.get("09. change")));

        // Remove the '%' character from change percent
        String changePercentStr = quoteData.get("10. change percent");
        if (changePercentStr.endsWith("%")) {
            changePercentStr = changePercentStr.substring(0, changePercentStr.length() - 1);
        }
        quote.setChangePercent(new BigDecimal(changePercentStr));

        quote.setTimestamp(LocalDateTime.now());
        return quote;
    }

    // Method to search for stocks by keyword
    public Map<String, Object> searchStocks(String keywords) {
        String url = "https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords={keywords}&apikey={apiKey}";

        try {
            return restTemplate.getForObject(url, Map.class, keywords, apiKey);
        } catch (Exception e) {
            log.error("Error searching for stocks with keywords {}: {}", keywords, e.getMessage());
            throw new RuntimeException("Failed to search stocks", e);
        }
    }

    // Optional: Method to get company overview (sector, industry, etc.)
    public Map<String, Object> getCompanyOverview(String ticker) {
        String url = "https://www.alphavantage.co/query?function=OVERVIEW&symbol={symbol}&apikey={apiKey}";

        try {
            return restTemplate.getForObject(url, Map.class, ticker, apiKey);
        } catch (Exception e) {
            log.error("Error fetching company overview for {}: {}", ticker, e.getMessage());
            throw new RuntimeException("Failed to fetch company data", e);
        }
    }
}