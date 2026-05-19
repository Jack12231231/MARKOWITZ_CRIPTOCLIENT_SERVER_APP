package com.example.criptoclient;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

public class MainController {

    @FXML private Label totalValueLabel;
    @FXML private Label returnLabel;
    @FXML private Label riskLabel;
    @FXML private LineChart<String, Number> portfolioChart;
    @FXML private TableView<?> assetsTable;

    @FXML
    public void initialize() {
        // Пример данных графика (можно потом подключить реальные)
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Портфель");
        series.getData().add(new XYChart.Data<>("Пн", 112000));
        series.getData().add(new XYChart.Data<>("Вт", 118500));
        series.getData().add(new XYChart.Data<>("Ср", 115200));
        series.getData().add(new XYChart.Data<>("Чт", 124000));
        series.getData().add(new XYChart.Data<>("Пт", 128450));

        portfolioChart.getData().add(series);

        // Пример значений
        totalValueLabel.setText("$128 450");
        returnLabel.setText("+21.7%");
        riskLabel.setText("11.8%");
    }
}