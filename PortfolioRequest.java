package com.example.criptoserver;

import java.io.Serializable;
import java.util.Map; // Должен быть этот импорт

public class PortfolioRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    // Поле должно быть Map!
    public Map<String, Double> assets;
    public double targetRisk;

    public PortfolioRequest(Map<String, Double> assets, double targetRisk) {
        this.assets = assets;
        this.targetRisk = targetRisk;
    }
}