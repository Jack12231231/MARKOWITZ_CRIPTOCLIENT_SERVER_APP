package com.example.criptoserver;

public class PortfolioComposition extends AbstractEntity {
    private int portfolioId;
    private int assetId;
    private double assetWeight;

    public PortfolioComposition() {}
    public PortfolioComposition(int portfolioId, int assetId, double assetWeight) {
        this.portfolioId = portfolioId;
        this.assetId = assetId;
        this.assetWeight = assetWeight;
    }

    public int getPortfolioId() { return portfolioId; }
    public int getAssetId() { return assetId; }
    public double getAssetWeight() { return assetWeight; }

    @Override
    public boolean validate() {
        return portfolioId > 0 && assetId > 0 && assetWeight > 0 && assetWeight <= 1.0;
    }
}