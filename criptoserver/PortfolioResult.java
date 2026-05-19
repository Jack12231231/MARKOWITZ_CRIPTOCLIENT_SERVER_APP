package com.example.criptoserver;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class PortfolioResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Double> weights;        // веса (0.0 - 1.0)
    private final Map<String, Double> marketValues;   // стоимость в долларах
    private final Map<String, Double> amounts;        // количество монет
    private final double expectedReturn;
    private final double portfolioRisk;

    public PortfolioResult(Map<String, Double> weights,
                           Map<String, Double> marketValues,
                           Map<String, Double> amounts,
                           double expectedReturn,
                           double portfolioRisk) {
        this.weights = weights != null ? new HashMap<>(weights) : new HashMap<>();
        this.marketValues = marketValues != null ? new HashMap<>(marketValues) : new HashMap<>();
        this.amounts = amounts != null ? new HashMap<>(amounts) : new HashMap<>();
        this.expectedReturn = expectedReturn;
        this.portfolioRisk = portfolioRisk;
    }

    // Геттеры
    public Map<String, Double> getWeights() { return weights; }
    public Map<String, Double> getMarketValues() { return marketValues; }
    public Map<String, Double> getAmounts() { return amounts; }
    public double getExpectedReturn() { return expectedReturn; }
    public double getPortfolioRisk() { return portfolioRisk; }
}