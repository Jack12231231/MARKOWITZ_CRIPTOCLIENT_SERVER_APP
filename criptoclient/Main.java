package com.example.criptoclient;

import com.example.criptoserver.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main extends Application {

    private String currentToken = null;
    private String userRole = "USER";
    private PortfolioResult lastPortfolioResult = null;

    private final TextArea historyArea = new TextArea();
    private final Label resultLabel = new Label();
    private final TabPane mainTabPane = new TabPane();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("CriptoPortfolio — Марковиц 2026");
        Font.loadFont(getClass().getResourceAsStream("/fonts/SerialA-Input.ttf"), 14);

        // ====================== ЛОГИН ======================
        VBox loginPane = new VBox(25);
        loginPane.setPadding(new Insets(50));
        loginPane.setAlignment(Pos.CENTER);
        loginPane.setStyle("-fx-background-color: linear-gradient(to bottom, #0f0f1e, #0a0a14); -fx-background-radius: 28;");

        Label title = new Label("CRIPTO PORTFOLIO");
        title.setStyle("-fx-font-size: 42px; -fx-font-weight: 800; -fx-text-fill: #00ffcc;");

        Label subtitle = new Label("Markowitz Optimization 2026");
        subtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #8888bb; -fx-font-weight: 500;");

        TextField tfLogin = new TextField();
        tfLogin.setPromptText("Логин");
        tfLogin.setPrefHeight(54);
        tfLogin.setStyle("""
            -fx-background-color: #1c1c2a;
            -fx-text-fill: #ffffff;
            -fx-font-weight: 700;
            -fx-font-size: 16px;
            -fx-prompt-text-fill: #8888bb;
            -fx-background-radius: 16;
            -fx-border-color: #444466;
            -fx-border-width: 1.5;
            -fx-padding: 14px 18px;
        """);

        PasswordField tfPass = new PasswordField();
        tfPass.setPromptText("Пароль");
        tfPass.setPrefHeight(54);
        tfPass.setStyle(tfLogin.getStyle());

        Button btnLogin = createNeonButton("ВОЙТИ");
        Button btnRegister = createNeonButton("ЗАРЕГИСТРИРОВАТЬСЯ");

        Label status = new Label();
        status.setStyle("-fx-font-size: 14px; -fx-font-weight: 600;");

        loginPane.getChildren().addAll(title, subtitle, tfLogin, tfPass, btnLogin, btnRegister, status);

        Scene loginScene = new Scene(loginPane, 480, 520);
        loginScene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        btnLogin.setOnAction(e -> handleLogin(tfLogin.getText(), tfPass.getText(), status, primaryStage));
        btnRegister.setOnAction(e -> handleRegister(tfLogin.getText(), tfPass.getText(), status));

        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    private Button createNeonButton(String text) {
        Button btn = new Button(text);
        btn.setPrefHeight(54);
        btn.setStyle("""
            -fx-background-color: linear-gradient(#00ffaa, #00cc88);
            -fx-text-fill: #0a0a14;
            -fx-font-weight: 800;
            -fx-font-size: 15.5px;
            -fx-background-radius: 9999;
            -fx-effect: dropshadow(gaussian, #00ffaa, 18, 0.35, 0, 0);
        """);
        return btn;
    }

    private void handleLogin(String username, String password, Label status, Stage stage) {
        if (username.isEmpty() || password.isEmpty()) {
            status.setText("Введите логин и пароль");
            status.setStyle("-fx-text-fill: #ff6666;");
            return;
        }

        Request req = new Request("LOGIN", new LoginData(username, password));
        Response resp = ClientNetwork.getInstance().sendRequest(req);

        if (resp.success) {
            if (resp.data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> authData = (Map<String, String>) resp.data;
                currentToken = authData.get("token");
                userRole = authData.getOrDefault("role", "USER").toUpperCase();
            } else {
                currentToken = (String) resp.data;
                userRole = "USER";
            }

            status.setStyle("-fx-text-fill: #00ffaa;");
            status.setText("Вход выполнен успешно! Роль: " + userRole);

            refreshTabs(mainTabPane);

            BorderPane root = new BorderPane();
            root.setCenter(mainTabPane);

            Scene mainScene = new Scene(root, 1350, 950);
            mainScene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            stage.setScene(mainScene);
            stage.centerOnScreen();
        } else {
            status.setStyle("-fx-text-fill: #ff6666;");
            status.setText("Ошибка: " + resp.message);
        }
    }

    private void handleRegister(String username, String password, Label status) {
        if (username.isEmpty() || password.isEmpty()) {
            status.setText("Введите логин и пароль");
            status.setStyle("-fx-text-fill: #ff6666;");
            return;
        }
        Request req = new Request("REGISTER", new LoginData(username, password));
        Response resp = ClientNetwork.getInstance().sendRequest(req);
        if (resp.success) {
            status.setStyle("-fx-text-fill: #00ffaa;");
            status.setText("Регистрация успешна! Теперь войдите.");
        } else {
            status.setStyle("-fx-text-fill: #ff6666;");
            status.setText("Ошибка: " + resp.message);
        }
    }

    private void refreshTabs(TabPane pane) {
        pane.getTabs().clear();

        pane.getTabs().add(createOptimizationTab());   // Оптимизация
        pane.getTabs().add(createPortfolioTab());      // Портфолио (с кнопками)
        pane.getTabs().add(createMarketDataTab());
        pane.getTabs().add(createHistoryTab());

        if ("ADMIN".equals(userRole)) {
            pane.getTabs().add(createAdminTab());
        }
    }

    // ====================== ВКЛАДКА "ПОРТФОЛИО" ======================
    private Tab createPortfolioTab() {
        Tab tab = new Tab("Портфолио");
        tab.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");

        VBox layout = new VBox(30);
        layout.setPadding(new Insets(40));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #0f0f1a;");

        Label title = new Label("Управление портфелями");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #00ffcc;");

        Button btnSave = createBigButton("Сохранить текущий портфель");
        Button btnHistory = createBigButton("История моих портфелей");
        Button btnExport = createBigButton("Экспорт отчёта в CSV");

        btnSave.setOnAction(e -> savePortfolio());
        btnHistory.setOnAction(e -> showPortfolioHistory());
        btnExport.setOnAction(e -> exportReport());

        layout.getChildren().addAll(title, btnSave, btnHistory, btnExport);
        tab.setContent(layout);
        return tab;
    }

    private Button createBigButton(String text) {
        Button btn = new Button(text);
        btn.setPrefHeight(65);
        btn.setPrefWidth(420);
        btn.setStyle("""
            -fx-background-color: linear-gradient(#00ffaa, #00cc88);
            -fx-text-fill: #0a0a14;
            -fx-font-weight: 800;
            -fx-font-size: 17px;
            -fx-background-radius: 25;
            -fx-effect: dropshadow(gaussian, #00ffaa, 20, 0.4, 0, 0);
        """);
        return btn;
    }

    // ====================== МЕТОДЫ ДЛЯ "ПОРТФОЛИО" ======================
    private void savePortfolio() {
        if (currentToken == null || lastPortfolioResult == null) {
            showAlert("Ошибка", "Сначала выполните оптимизацию!", Alert.AlertType.ERROR);
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Мой портфель " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        data.put("targetRisk", lastPortfolioResult.getPortfolioRisk());
        data.put("weights", lastPortfolioResult.getWeights());

        Response resp = ClientNetwork.getInstance().sendRequest(new Request("SAVE_PORTFOLIO", data, currentToken));
        showAlert(resp.success ? "Успех" : "Ошибка", resp.message,
                resp.success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
    }

    private void showPortfolioHistory() {
        if (currentToken == null) {
            showAlert("Ошибка", "Сначала войдите в систему!", Alert.AlertType.ERROR);
            return;
        }

        Response resp = ClientNetwork.getInstance().sendRequest(new Request("GET_PORTFOLIO_HISTORY", null, currentToken));

        if (resp.success && resp.data instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> history = (List<Map<String, Object>>) resp.data;
            showHistoryWindow(history);
        } else {
            showAlert("Ошибка", resp.message != null ? resp.message : "Не удалось загрузить историю", Alert.AlertType.ERROR);
        }
    }

    private void exportReport() {
        if (currentToken == null || lastPortfolioResult == null) {
            showAlert("Ошибка", "Сначала выполните оптимизацию!", Alert.AlertType.WARNING);
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("weights", lastPortfolioResult.getWeights());

        Response resp = ClientNetwork.getInstance().sendRequest(new Request("EXPORT_REPORT", data, currentToken));
        showAlert(resp.success ? "Успех" : "Ошибка", resp.message,
                resp.success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
    }

    private void showHistoryWindow(List<Map<String, Object>> history) {
        StringBuilder sb = new StringBuilder("ИСТОРИЯ ВАШИХ ПОРТФЕЛЕЙ\n\n");
        for (Map<String, Object> p : history) {
            sb.append(" ").append(p.get("date"))
                    .append(" ").append(p.get("name"))
                    .append(" Риск: ").append(p.get("targetRisk"))
                    .append("\nСостав: ").append(p.get("composition"))
                    .append("\n\n");
        }

        TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        area.setWrapText(true);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("История портфелей");
        dialog.getDialogPane().setContent(area);
        dialog.getDialogPane().setPrefSize(750, 650);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.show();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    private void killSelectedSession(ListView<String> lv) {
        String selected = lv.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showNotice("Ошибка", "Выберите пользователя в списке");
            return;
        }

        String username = selected.split("\\s+\\[")[0].trim();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Отключить все сессии пользователя " + username + "?", ButtonType.YES, ButtonType.NO);

        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            Response r = ClientNetwork.getInstance().sendRequest(
                    new Request("KILL_SESSION", username, currentToken)
            );
            showNotice("Результат", r != null ? r.message : "Нет ответа от сервера");
            refreshUserList(lv);   // ← исправлено
        }
    }

    private void deleteSelectedUser(ListView<String> lv) {
        String selected = lv.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showNotice("Ошибка", "Выберите пользователя в списке");
            return;
        }

        String username = selected.split("\\s+\\[")[0].trim();

        if ("admin".equalsIgnoreCase(username)) {
            showNotice("Ошибка", "Нельзя удалить администратора!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.WARNING,
                "ВНИМАНИЕ! Полностью удалить пользователя " + username +
                        " и все его портфели?\n\nЭто действие необратимо!",
                ButtonType.YES, ButtonType.NO);

        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            Response r = ClientNetwork.getInstance().sendRequest(
                    new Request("DELETE_USER", username, currentToken)
            );
            showNotice("Результат", r != null ? r.message : "Нет ответа от сервера");
            refreshUserList(lv);   // ← исправлено
        }
    }

    // Добавь этот вспомогательный метод
    private void refreshUserList(ListView<String> lv) {
        Response r = ClientNetwork.getInstance().sendRequest(new Request("GET_USERS", null, currentToken));
        if (r != null && r.success && r.data instanceof List) {
            lv.getItems().setAll((List<String>) r.data);
        } else {
            lv.getItems().setAll("Ошибка загрузки списка пользователей");
        }
    }

    // ====================== ВКЛАДКА ОПТИМИЗАЦИИ ======================
    private Tab createOptimizationTab() {
        Tab tab = new Tab("Оптимизация и Доходность");
        tab.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");

        HBox layout = new HBox(35);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #0f0f1a;");

        VBox inputSide = new VBox(22);
        inputSide.setPrefWidth(430);

        Label titleLabel = new Label("Ваши активы (Тикер:Количество):");
        titleLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: 700; -fx-text-fill: #00ffcc;");

        TextField tfAssets = new TextField("BTC:0.01, TON:500");
        tfAssets.setPrefHeight(54);
        tfAssets.setStyle("""
            -fx-background-color: #1c1c2a;
            -fx-text-fill: #ffffff;
            -fx-font-weight: 700;
            -fx-font-size: 16px;
            -fx-prompt-text-fill: #8888bb;
            -fx-background-radius: 16;
            -fx-border-color: #444466;
            -fx-border-width: 1.5;
            -fx-padding: 14px 18px;
        """);

        Label riskTitle = new Label("Целевой риск:");
        riskTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 700; -fx-text-fill: #00ffcc;");

        Slider riskSlider = new Slider(0, 1.0, 0.25);
        riskSlider.setPrefWidth(380);

        Label riskLabel = new Label("Целевой риск: 0.25");
        riskLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #bbbbdd;");

        riskSlider.valueProperty().addListener((o, old, n) ->
                riskLabel.setText(String.format("Целевой риск: %.2f", n.doubleValue())));

        Button btnCalc = new Button("РАССЧИТАТЬ ПО МАРКОВИЦУ");
        btnCalc.setStyle("""
            -fx-background-color: linear-gradient(#00ffaa, #00cc88);
            -fx-text-fill: #0a0a14;
            -fx-font-weight: 800;
            -fx-font-size: 15.5px;
            -fx-background-radius: 50;
            -fx-padding: 15px 42px;
        """);

        inputSide.getChildren().addAll(titleLabel, tfAssets, riskTitle, riskLabel, riskSlider, btnCalc, new Label("Результат:"), resultLabel);

        VBox chartSide = new VBox(25);
        chartSide.setAlignment(Pos.CENTER);

        PieChart pieChart = new PieChart();
        pieChart.setTitle("Структура портфеля по Марковицу (%)");
        pieChart.setPrefSize(480, 400);
        pieChart.setStyle("-fx-background-color: #161622; -fx-background-radius: 20;");

        BarChart<String, Number> barChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        barChart.setTitle("Рыночная стоимость активов в долларах");
        barChart.setPrefHeight(380);
        barChart.setStyle("-fx-background-color: #161622; -fx-background-radius: 20;");

        chartSide.getChildren().addAll(pieChart, barChart);

        layout.getChildren().addAll(inputSide, chartSide);
        tab.setContent(layout);

        btnCalc.setOnAction(e -> {
            if (currentToken == null) {
                new Alert(Alert.AlertType.ERROR, "Сначала войдите в систему!").show();
                return;
            }

            try {
                String input = tfAssets.getText().trim();
                String[] parts = input.split(",");
                Map<String, Double> assetsMap = new HashMap<>();

                for (String p : parts) {
                    String[] kv = p.trim().split(":");
                    if (kv.length == 2) {
                        String ticker = kv[0].toUpperCase().trim();
                        double amount = Double.parseDouble(kv[1].trim());
                        assetsMap.put(ticker, amount);
                    }
                }

                if (assetsMap.isEmpty()) throw new Exception("Введите хотя бы один актив");

                double targetRisk = riskSlider.getValue();

                PortfolioRequest reqData = new PortfolioRequest(assetsMap, targetRisk);
                Request req = new Request("OPTIMIZE", reqData, currentToken);

                Response response = ClientNetwork.getInstance().sendRequest(req);

                if (response.success && response.data instanceof PortfolioResult pr) {
                    lastPortfolioResult = pr;

                    Map<String, Double> weights = pr.getWeights();
                    Map<String, Double> marketValues = pr.getMarketValues();
                    Map<String, Double> amounts = pr.getAmounts();

                    double totalValue = marketValues.values().stream().mapToDouble(Double::doubleValue).sum();

                    pieChart.getData().clear();
                    for (Map.Entry<String, Double> entry : weights.entrySet()) {
                        String symbol = entry.getKey();
                        double weight = entry.getValue();
                        PieChart.Data slice = new PieChart.Data(symbol, weight * 100);
                        slice.nameProperty().set(symbol + " (" + String.format("%.1f%%", weight * 100) + ")");
                        pieChart.getData().add(slice);
                    }

                    barChart.getData().clear();
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Стоимость в $");
                    for (Map.Entry<String, Double> entry : marketValues.entrySet()) {
                        series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                    }
                    barChart.getData().add(series);

                    StringBuilder sb = new StringBuilder();
                    sb.append("Оптимизация по Марковицу завершена!\n\n");
                    sb.append(String.format("Ожидаемая годовая доходность: %.2f%%\n", pr.getExpectedReturn() * 100));
                    sb.append(String.format("Риск портфеля: %.4f\n", pr.getPortfolioRisk()));
                    sb.append(String.format("Общая стоимость портфеля: $%,.2f\n", totalValue));
                    sb.append(String.format("Ожидаемая прибыль: $%,.2f\n\n", pr.getExpectedReturn() * totalValue));
                    sb.append("Вклад каждого актива:\n");

                    for (String symbol : weights.keySet()) {
                        double weight = weights.get(symbol);
                        double amount = amounts.getOrDefault(symbol, 0.0);
                        double valueUsd = marketValues.getOrDefault(symbol, 0.0);
                        sb.append(String.format("• %s: %.4f %s — $%,.2f (%.1f%%)\n", symbol, amount, symbol, valueUsd, weight * 100));
                    }

                    resultLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #00ffcc; -fx-font-weight: 600;");
                    resultLabel.setText(sb.toString());

                    addToHistory(input, targetRisk);
                } else {
                    resultLabel.setStyle("-fx-text-fill: #ff6666; -fx-font-weight: 600;");
                    resultLabel.setText("Ошибка: " + response.message);
                }
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Ошибка ввода!\nПример: BTC:0.01, TON:500\n\n" + ex.getMessage()).show();
            }
        });

        return tab;
    }

    // ====================== ОСТАЛЬНЫЕ ВКЛАДКИ ======================
    private Tab createMarketDataTab() {
        Tab tab = new Tab("Курсы");
        tab.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");

        VBox layout = new VBox(20);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #0f0f1a;");

        Label header = new Label("Текущие котировки криптовалют");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #00ffcc;");

        Button refreshBtn = new Button("Обновить курсы");
        refreshBtn.setStyle("""
            -fx-background-color: linear-gradient(#00ffaa, #00cc88);
            -fx-text-fill: #0a0a14;
            -fx-font-weight: 700;
            -fx-font-size: 15px;
            -fx-background-radius: 50;
            -fx-padding: 12px 34px;
        """);

        ListView<String> marketList = new ListView<>();
        marketList.setPrefHeight(650);
        marketList.setStyle("-fx-background-color: #1c1c2a; -fx-control-inner-background: #1c1c2a;");

        marketList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-background-color: #1c1c2a; -fx-text-fill: #e0e0ff; -fx-font-weight: 700; -fx-font-size: 15.5px; -fx-padding: 12px 15px;");
                }
            }
        });

        refreshBtn.setOnAction(e -> fetchMarketData(marketList));
        tab.setOnSelectionChanged(ev -> {
            if (tab.isSelected()) fetchMarketData(marketList);
        });

        layout.getChildren().addAll(header, refreshBtn, marketList);
        tab.setContent(layout);
        return tab;
    }

    private Tab createHistoryTab() {
        Tab tab = new Tab("История");
        tab.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #0f0f1a;");

        Label header = new Label("Журнал расчётов портфеля");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #00ffcc;");

        historyArea.setEditable(false);
        historyArea.setPrefHeight(680);
        historyArea.setStyle("""
            -fx-background-color: #1c1c2a;
            -fx-control-inner-background: #1c1c2a;
            -fx-text-fill: #aaddff;
            -fx-font-weight: 700;
            -fx-font-size: 15px;
            -fx-padding: 18px;
        """);

        layout.getChildren().addAll(header, historyArea);
        tab.setContent(layout);
        return tab;
    }

    private Tab createAdminTab() {
        Tab tab = new Tab("Админ-Панель");
        tab.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");

        HBox layout = new HBox(40);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #0f0f1a;");

        // ====================== ЛЕВАЯ ЧАСТЬ - РАБОТА С АКТИВАМИ ======================
        VBox dbBox = new VBox(16);
        dbBox.setPrefWidth(380);

        Label dbTitle = new Label("Редактирование / Добавление актива:");
        dbTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #00ffcc;");

        TextField tfS = new TextField();
        tfS.setPromptText("Тикер (напр. BTC)");
        tfS.setStyle("""
            -fx-background-color: #1c1c2a;
            -fx-text-fill: #ffffff;
            -fx-font-weight: 700;
            -fx-font-size: 15px;
            -fx-prompt-text-fill: #8888bb;
            -fx-background-radius: 14px;
            -fx-border-color: #444466;
            -fx-border-width: 1.5;
            -fx-padding: 12px 16px;
        """);

        TextField tfName = new TextField();
        tfName.setPromptText("Полное название (напр. Bitcoin)");
        tfName.setStyle(tfS.getStyle());

        TextField tfP = new TextField();
        tfP.setPromptText("Цена");
        tfP.setStyle(tfS.getStyle());

        TextField tfC = new TextField();
        tfC.setPromptText("Ковариация");
        tfC.setStyle(tfS.getStyle());

        Button bSave = new Button("Сохранить в БД");
        bSave.setPrefWidth(320);
        bSave.setPrefHeight(52);
        bSave.setStyle("""
            -fx-background-color: linear-gradient(#00ffaa, #00cc88);
            -fx-text-fill: #0a0a14;
            -fx-font-weight: 800;
            -fx-font-size: 15.5px;
            -fx-background-radius: 50;
            -fx-effect: dropshadow(gaussian, #00ffaa, 18, 0.4, 0, 0);
        """);

        Button bDelete = new Button("Удалить актив из БД");
        bDelete.setPrefWidth(320);
        bDelete.setPrefHeight(52);
        bDelete.setStyle("""
            -fx-background-color: linear-gradient(#ff4444, #cc2222);
            -fx-text-fill: white;
            -fx-font-weight: 800;
            -fx-font-size: 15.5px;
            -fx-background-radius: 50;
            -fx-effect: dropshadow(gaussian, #ff4444, 20, 0.5, 0, 0);
        """);

        bSave.setOnAction(e -> { /* твой код сохранения актива */ });
        bDelete.setOnAction(e -> { /* твой код удаления актива */ });

        dbBox.getChildren().addAll(
                dbTitle, new Label("Тикер:"), tfS,
                new Label("Название:"), tfName,
                new Label("Цена:"), tfP,
                new Label("Риск (ковариация):"), tfC,
                bSave, bDelete
        );

        // ====================== ПРАВАЯ ЧАСТЬ - УПРАВЛЕНИЕ СЕССИЯМИ ======================
        VBox userBox = new VBox(16);
        userBox.setPrefWidth(380);

        Label userTitle = new Label("Управление сессиями:");
        userTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #00ffcc;");

        ListView<String> lv = new ListView<>();
        lv.setPrefHeight(380);
        lv.setStyle("""
            -fx-background-color: #1c1c2a;
            -fx-control-inner-background: #1c1c2a;
            -fx-text-fill: #e0e0ff;
            -fx-font-weight: 700;
            -fx-font-size: 15px;
            -fx-border-color: #333355;
            -fx-border-width: 1.5;
        """);

        // Подсветка выбранного элемента
        lv.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                lv.setStyle("""
                    -fx-background-color: #1c1c2a;
                    -fx-control-inner-background: #1c1c2a;
                    -fx-text-fill: #e0e0ff;
                    -fx-font-weight: 700;
                    -fx-font-size: 15px;
                    -fx-border-color: #00ffcc;
                    -fx-border-width: 2;
                """);
            }
        });

        Button bRef = new Button("Обновить список");
        bRef.setPrefWidth(320);
        bRef.setPrefHeight(52);
        bRef.setStyle("""
            -fx-background-color: linear-gradient(#00ffaa, #00cc88);
            -fx-text-fill: #0a0a14;
            -fx-font-weight: 800;
            -fx-font-size: 15.5px;
            -fx-background-radius: 50;
            -fx-effect: dropshadow(gaussian, #00ffaa, 15, 0.4, 0, 0);
        """);

        Button btnKillSession = new Button("Отключить выбранную сессию");
        btnKillSession.setPrefWidth(320);
        btnKillSession.setPrefHeight(52);
        btnKillSession.setStyle("""
            -fx-background-color: linear-gradient(#ffaa00, #cc8800);
            -fx-text-fill: white;
            -fx-font-weight: 800;
            -fx-font-size: 15px;
            -fx-background-radius: 50;
            -fx-effect: dropshadow(gaussian, #ffaa00, 18, 0.4, 0, 0);
        """);

        Button btnDeleteUser = new Button("Удалить пользователя");
        btnDeleteUser.setPrefWidth(320);
        btnDeleteUser.setPrefHeight(52);
        btnDeleteUser.setStyle("""
            -fx-background-color: linear-gradient(#ff4444, #cc2222);
            -fx-text-fill: white;
            -fx-font-weight: 800;
            -fx-font-size: 15px;
            -fx-background-radius: 50;
            -fx-effect: dropshadow(gaussian, #ff4444, 20, 0.5, 0, 0);
        """);

        bRef.setOnAction(ev -> refreshUserList(lv));
        btnKillSession.setOnAction(e -> killSelectedSession(lv));
        btnDeleteUser.setOnAction(e -> deleteSelectedUser(lv));

        userBox.getChildren().addAll(userTitle, lv, bRef, btnKillSession, btnDeleteUser);

        layout.getChildren().addAll(dbBox, userBox);
        tab.setContent(layout);
        return tab;
    }

    private void showNotice(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    private void fetchMarketData(ListView<String> lv) {
        if (currentToken == null) {
            lv.getItems().setAll("Ошибка: Сначала войдите в систему");
            return;
        }
        Request req = new Request("GET_MARKET_DATA", null, currentToken);
        Response response = ClientNetwork.getInstance().sendRequest(req);

        lv.getItems().clear();
        if (response.success && response.data instanceof Map) {
            ((Map<String, Double>) response.data).forEach((k, v) ->
                    lv.getItems().add(String.format("%s: $%,.2f", k, v)));
        } else {
            lv.getItems().add("Не удалось загрузить курсы: " + response.message);
        }
    }

    private void addToHistory(String data, double risk) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Platform.runLater(() -> historyArea.appendText(
                String.format("[%s] Активы: %s | Риск: %.2f\n", date, data, risk)));
    }

    public static void main(String[] args) { launch(args); }
}