package com.example.criptoserver;

public class AssetCovariance extends AbstractEntity {
    private int assetAId;
    private int assetBId;
    private double covarianceValue;

    public AssetCovariance() {}
    public AssetCovariance(int assetAId, int assetBId, double covarianceValue) {
        this.assetAId = assetAId;
        this.assetBId = assetBId;
        this.covarianceValue = covarianceValue;
    }

    public int getAssetAId() { return assetAId; }
    public int getAssetBId() { return assetBId; }
    public double getCovarianceValue() { return covarianceValue; }

    @Override
    public boolean validate() {
        return assetAId > 0 && assetBId > 0;
    }
}