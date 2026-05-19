package com.example.criptoserver;

public class OptimizerFactory {

    public static PortfolioOptimizer getOptimizer() {
        return new MarkowitzOptimizer();   // возвращаем реальную реализацию
    }
}