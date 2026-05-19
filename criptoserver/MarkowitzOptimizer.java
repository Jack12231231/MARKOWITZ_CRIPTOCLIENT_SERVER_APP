package com.example.criptoserver;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MarkowitzOptimizer implements PortfolioOptimizer {

    @Override
    public PortfolioResult optimize(Map<String, Double> assets, double targetRisk) {
        if (assets == null || assets.isEmpty()) {
            return new PortfolioResult(new HashMap<>(), new HashMap<>(), new HashMap<>(), 0.0, 0.0);
        }

        List<String> symbols = new ArrayList<>(assets.keySet());

        Map<String, Double> prices = loadPricesFromDB(symbols);
        Map<String, Double> expectedReturns = loadExpectedReturnsFromDB(symbols);
        double[][] covMatrix = loadFullCovarianceMatrix(symbols);   // ← ИСПРАВЛЕНО

        // Расчёт рыночной стоимости
        Map<String, Double> marketValues = new HashMap<>();
        Map<String, Double> amountsMap = new HashMap<>(assets);
        double totalValue = 0.0;

        for (Map.Entry<String, Double> entry : assets.entrySet()) {
            String symbol = entry.getKey().toUpperCase();
            double amount = entry.getValue();
            double price = prices.getOrDefault(symbol, 0.0);
            double mv = amount * price;

            marketValues.put(symbol, mv);
            totalValue += mv;
        }

        // === Оптимизация весов (твоя логика + улучшения) ===
        double sensitivity = 1.7 / (targetRisk + 0.09);

        double[] rawWeights = new double[symbols.size()];
        double totalRaw = 0.0;

        for (int i = 0; i < symbols.size(); i++) {
            String s = symbols.get(i).toUpperCase();
            double mv = marketValues.getOrDefault(s, 0.0);
            double er = expectedReturns.getOrDefault(s, 0.18);
            double var = covMatrix[i][i];                    // дисперсия

            rawWeights[i] = mv * (er / Math.pow(var, sensitivity));
            totalRaw += rawWeights[i];
        }

        Map<String, Double> weights = new LinkedHashMap<>();
        for (int i = 0; i < symbols.size(); i++) {
            double w = (totalRaw > 0) ? rawWeights[i] / totalRaw : 0.0;
            weights.put(symbols.get(i), Math.round(w * 1000.0) / 1000.0);
        }

        // Расчёт риска с учётом ковариаций между активами
        double expReturn = 0.0;
        double risk = 0.0;

        for (int i = 0; i < symbols.size(); i++) {
            String s = symbols.get(i).toUpperCase();
            double w = weights.getOrDefault(s, 0.0);
            expReturn += w * expectedReturns.getOrDefault(s, 0.18);

            for (int j = 0; j < symbols.size(); j++) {
                risk += w * weights.getOrDefault(symbols.get(j).toUpperCase(), 0.0) * covMatrix[i][j];
            }
        }
        risk = Math.sqrt(Math.max(risk, 0));

        PortfolioResult result = new PortfolioResult(weights, marketValues, amountsMap, expReturn, risk);

        saveToHistory(weights, risk);
        exportReport(weights);

        return result;
    }

    // ====================== ПОЛНАЯ КОВАРИАЦИОННАЯ МАТРИЦА ======================
    private double[][] loadFullCovarianceMatrix(List<String> symbols) {
        int n = symbols.size();
        double[][] matrix = new double[n][n];

        String sql = """
            SELECT ca1.symbol as sym1, ca2.symbol as sym2, ac.covariance_value
            FROM AssetCovariance ac
            JOIN CryptoAsset ca1 ON ac.asset_a_id = ca1.id
            JOIN CryptoAsset ca2 ON ac.asset_b_id = ca2.id
            WHERE ca1.symbol IN (SELECT symbol FROM CryptoAsset WHERE symbol = ?)
              AND ca2.symbol IN (SELECT symbol FROM CryptoAsset WHERE symbol = ?)""";

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    matrix[i][j] = getCovariance(conn, symbols.get(i), symbols.get(j));
                }
            }
        } catch (Exception e) {
            System.out.println("Ошибка загрузки ковариационной матрицы: " + e.getMessage());
        }
        return matrix;
    }

    private double getCovariance(Connection conn, String symA, String symB) {
        String sql = """
            SELECT covariance_value 
            FROM AssetCovariance ac
            JOIN CryptoAsset ca1 ON ac.asset_a_id = ca1.id
            JOIN CryptoAsset ca2 ON ac.asset_b_id = ca2.id
            WHERE ca1.symbol = ? AND ca2.symbol = ?""";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symA.toUpperCase());
            ps.setString(2, symB.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("covariance_value");
            }
        } catch (Exception ignored) {}

        return symA.equalsIgnoreCase(symB) ? 0.12 : 0.05; // дефолт
    }

    // ====================== ЗАГРУЗКА ЦЕН ТОЛЬКО ИЗ БД (без дефолтов) ======================

    private Map<String, Double> loadPricesFromDB(List<String> symbols) {
        Map<String, Double> map = new HashMap<>();
        String sql = "SELECT symbol, current_price FROM CryptoAsset WHERE symbol = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (String s : symbols) {
                ps.setString(1, s.toUpperCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        double price = rs.getDouble("current_price");
                        map.put(s.toUpperCase(), price);        // даже если 0 — берём 0
                    } else {
                        map.put(s.toUpperCase(), 0.0);          // актива нет в базе → цена 0
                        System.out.println("ВНИМАНИЕ: Актив " + s + " не найден в CryptoAsset");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Ошибка загрузки цен из БД: " + e.getMessage());
        }
        return map;
    }

    private Map<String, Double> loadVariancesFromDB(List<String> symbols) {
        Map<String, Double> map = new HashMap<>();
        String sql = "SELECT ca.symbol, ac.covariance_value FROM AssetCovariance ac " +
                "JOIN CryptoAsset ca ON ac.asset_a_id = ca.id " +
                "WHERE ca.symbol = ? AND ac.asset_b_id = ac.asset_a_id";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (String s : symbols) {
                ps.setString(1, s.toUpperCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        map.put(s.toUpperCase(), rs.getDouble("covariance_value"));
                    } else {
                        map.put(s.toUpperCase(), 0.10);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Ошибка загрузки variance: " + e.getMessage());
        }
        return map;
    }

    private Map<String, Double> loadExpectedReturnsFromDB(List<String> symbols) {
        Map<String, Double> map = new HashMap<>();
        map.put("BTC", 0.15);
        map.put("ETH", 0.18);
        map.put("SOL", 0.25);
        map.put("TON", 0.22);

        for (String s : symbols) {
            map.putIfAbsent(s.toUpperCase(), 0.18);
        }
        return map;
    }

    // ====================== Сохранение ======================

    private void saveToHistory(Map<String, Double> weights, double risk) {
        String sql = "INSERT INTO SavedPortfolios (portfolio_data, target_risk) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, weights.toString());
            ps.setDouble(2, risk);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    private void exportReport(Map<String, Double> weights) {
        try (PrintWriter pw = new PrintWriter(new File("portfolio_report.csv"))) {
            pw.println("Asset;Weight(%)");
            for (var e : weights.entrySet()) {
                pw.printf("%s;%.2f%%\n", e.getKey(), e.getValue() * 100);
            }
        } catch (Exception ignored) {}
    }
}