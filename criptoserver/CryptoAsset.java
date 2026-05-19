package com.example.criptoserver;

public class CryptoAsset extends AbstractEntity {
    private String symbol;
    private String name;

    public CryptoAsset() {}

    public CryptoAsset(String symbol, String name) {
        this.symbol = symbol;
        this.name = name;
    }

    // === ГЕТТЕРЫ (были) ===
    public String getSymbol() { return symbol; }
    public String getName() { return name; }

    // === СЕТТЕРЫ (добавлены) ===
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean validate() {
        return symbol != null && !symbol.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "CryptoAsset{id=" + getId() + ", symbol='" + symbol + "', name='" + name + "'}";
    }
}