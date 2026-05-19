package com.example.criptoserver;

import java.time.LocalDateTime;

public class Portfolio extends AbstractEntity {
    private int userId;
    private String name;
    private double targetRisk;
    private LocalDateTime createdAt;

    public Portfolio() {}

    public Portfolio(int userId, String name, double targetRisk) {
        this.userId = userId;
        this.name = name;
        this.targetRisk = targetRisk;
        this.createdAt = LocalDateTime.now();
    }

    // === ГЕТТЕРЫ (были) ===
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public double getTargetRisk() { return targetRisk; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // === СЕТТЕРЫ (добавлены) ===
    public void setUserId(int userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setTargetRisk(double targetRisk) { this.targetRisk = targetRisk; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean validate() {
        return userId > 0 && targetRisk >= 0;
    }

    @Override
    public String toString() {
        return "Portfolio{id=" + getId() + ", name='" + name + "', risk=" + targetRisk + "}";
    }
}