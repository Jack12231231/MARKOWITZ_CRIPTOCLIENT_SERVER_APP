package com.example.criptoserver;

import java.util.Map;

public interface PortfolioOptimizer {
    PortfolioResult optimize(Map<String, Double> assets, double targetRisk);
}