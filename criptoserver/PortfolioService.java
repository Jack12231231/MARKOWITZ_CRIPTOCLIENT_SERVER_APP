package com.example.criptoserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class PortfolioService {

    // Сохранение в базу данных
    public static void saveToDatabase(Map<String, Double> result, double risk) {
        String sql = "INSERT INTO SavedPortfolios (portfolio_data, target_risk) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, result.toString());
            pstmt.setDouble(2, risk);
            pstmt.executeUpdate();
            System.out.println("Сервер: Портфель успешно сохранен в историю.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Экспорт в файл (для Excel)
    public static void exportToCSV(Map<String, Double> result) {
        File file = new File("portfolio_report.csv");
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("Asset;Weight(%)");
            for (Map.Entry<String, Double> entry : result.entrySet()) {
                writer.printf("%s;%.2f%%%n", entry.getKey(), entry.getValue() * 100);
            }
            System.out.println("Сервер: Отчет создан: " + file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}