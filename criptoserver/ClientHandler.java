package com.example.criptoserver;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.sql.*;
import java.util.*;

public class ClientHandler implements Runnable {

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            while (true) {
                Object input = in.readObject();
                if (input instanceof Request req) {
                    // Если сессия была удалена админом, отклоняем любые запросы кроме LOGIN
                    if (req.token != null && !Server.activeTokens.containsKey(req.token) && !req.command.equals("LOGIN")) {
                        out.writeObject(new Response(false, "SESSION_KILLED", null));
                        out.flush();
                        break; // Разрываем соединение
                    }

                    Response response = processRequest(req);
                    out.writeObject(response);
                    out.flush();
                    out.reset();
                }
            }
        } catch (EOFException | SocketException e) {
            System.out.println("Клиент " + socket.getInetAddress() + " отключился");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private Response processRequest(Request req) {
        return switch (req.command) {
            case "LOGIN" -> handleLogin(req);
            case "REGISTER" -> handleRegister(req);
            case "GET_MARKET_DATA" -> handleGetMarketData(req);
            case "OPTIMIZE" -> handleOptimize(req);
            case "SAVE_PORTFOLIO" -> handleSavePortfolio(req);
            case "GET_PORTFOLIO_HISTORY" -> handleGetPortfolioHistory(req);
            case "EXPORT_REPORT" -> handleExportReport(req);

            // Админские команды
            case "UPDATE_ASSET" -> handleUpdateAsset(req);
            case "GET_USERS" -> handleGetUsers(req);
            case "KILL_SESSION" -> handleKillSession(req);
            case "UPDATE_ASSET_DB" -> handleUpdateAssetDB(req);
            case "DELETE_ASSET" -> handleDeleteAsset(req);
            case "DELETE_USER" -> handleDeleteUser(req);
            default -> new Response(false, "Команда не найдена: " + req.command, null);
        };
    }

    private Response handleLogin(Request req) {
        if (!(req.data instanceof LoginData ld)) return new Response(false, "Ошибка данных", null);

        String sql = "SELECT u.username, r.name_role FROM AppUser u " +
                "JOIN Role r ON u.role_id = r.id WHERE u.username = ? AND u.password_hash = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ld.username);
            pstmt.setString(2, ld.password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String token = UUID.randomUUID().toString();
                String role = rs.getString("name_role");

                Server.activeTokens.put(token, ld.username);
                Server.tokenToRole.put(token, role);

                // Возвращаем и токен, и роль для настройки интерфейса клиента
                Map<String, String> authData = new HashMap<>();
                authData.put("token", token);
                authData.put("role", role);

                return new Response(true, "Вход выполнен успешно", authData);
            }
            return new Response(false, "Неверный логин или пароль", null);
        } catch (SQLException e) {
            return new Response(false, "Ошибка БД: " + e.getMessage(), null);
        }
    }

    private Response handleRegister(Request req) {
        if (!(req.data instanceof LoginData ld)) return new Response(false, "Ошибка данных", null);

        String sql = "INSERT INTO AppUser (username, password_hash, role_id) " +
                "VALUES (?, ?, (SELECT id FROM Role WHERE name_role = 'USER' LIMIT 1))";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ld.username);
            pstmt.setString(2, ld.password);
            pstmt.executeUpdate();
            return new Response(true, "Регистрация прошла успешно", null);
        } catch (SQLException e) {
            return new Response(false, "Ошибка регистрации: " + e.getMessage(), null);
        }
    }

    private Response handleGetMarketData(Request req) {
        if (!isAuthorized(req.token, null)) return new Response(false, "Неавторизован", null);

        // Выбираем символ и цену для ВСЕХ активов
        String sql = "SELECT symbol, current_price FROM cryptoasset";
        Map<String, Double> marketData = new HashMap<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String sym = rs.getString("symbol");
                double price = rs.getDouble("current_price");
                marketData.put(sym, price);
            }

            // Если мапа пустая, золото не добавилось или запрос его не видит
            if (marketData.isEmpty()) return new Response(false, "Данные в БД не найдены", null);

            return new Response(true, "Данные получены", marketData);
        } catch (SQLException e) {
            return new Response(false, "Ошибка БД: " + e.getMessage(), null);
        }
    }

    private Response handleUpdateAsset(Request req) {
        if (!isAuthorized(req.token, "ADMIN")) return new Response(false, "Доступ запрещен", null);
        if (!(req.data instanceof Map<?, ?> data)) return new Response(false, "Неверный формат данных", null);

        // Обновляем цену и риск_скор (согласно структуре вашей таблицы)
        String sql = "UPDATE cryptoasset SET current_price = ?, risk_score = ? WHERE symbol = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, Double.parseDouble(data.get("price").toString()));
            pstmt.setDouble(2, Double.parseDouble(data.get("risk").toString()));
            pstmt.setString(3, data.get("symbol").toString());

            int affected = pstmt.executeUpdate();
            if (affected > 0) return new Response(true, "Данные актива обновлены", null);
            else return new Response(false, "Актив не найден", null);
        } catch (Exception e) {
            return new Response(false, "Ошибка обновления: " + e.getMessage(), null);
        }
    }

    private Response handleGetUsers(Request req) {
        if (!isAuthorized(req.token, "ADMIN")) return new Response(false, "Доступ запрещен", null);

        // Список всех пользователей из БД + пометка, кто сейчас в сети
        List<String> usersInfo = new ArrayList<>();
        String sql = "SELECT username FROM AppUser";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String user = rs.getString("username");
                boolean isOnline = Server.activeTokens.containsValue(user);
                usersInfo.add(user + (isOnline ? " [В СЕТИ]" : " [ОФФЛАЙН]"));
            }
            return new Response(true, "Список пользователей получен", usersInfo);
        } catch (SQLException e) {
            return new Response(false, "Ошибка БД", null);
        }
    }

    private Response handleKillSession(Request req) {
        if (!isAuthorized(req.token, "ADMIN"))
            return new Response(false, "Доступ запрещен", null);

        if (!(req.data instanceof String targetUsername))
            return new Response(false, "Неверный формат данных", null);

        // Удаляем все активные токены этого пользователя
        boolean removed = Server.activeTokens.entrySet().removeIf(entry ->
                entry.getValue().equals(targetUsername));

        // Также чистим роли
        Server.tokenToRole.entrySet().removeIf(entry ->
                Server.activeTokens.get(entry.getKey()) == null); // на всякий случай

        if (removed) {
            return new Response(true, "Все сессии пользователя " + targetUsername + " отключены", null);
        } else {
            return new Response(false, "Пользователь не найден в активных сессиях", null);
        }
    }
    private Response handleDeleteUser(Request req) {
        if (!isAuthorized(req.token, "ADMIN"))
            return new Response(false, "Доступ запрещен", null);

        if (!(req.data instanceof String username))
            return new Response(false, "Неверный формат данных", null);

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Удаляем составы портфелей
                String sql1 = """
                    DELETE FROM portfoliocomposition 
                    WHERE portfolio_id IN (SELECT id FROM portfolio WHERE user_id = 
                        (SELECT id FROM AppUser WHERE username = ?));
                    """;
                try (PreparedStatement ps = conn.prepareStatement(sql1)) {
                    ps.setString(1, username);
                    ps.executeUpdate();
                }

                // 2. Удаляем сами портфели
                String sql2 = "DELETE FROM portfolio WHERE user_id = (SELECT id FROM AppUser WHERE username = ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql2)) {
                    ps.setString(1, username);
                    ps.executeUpdate();
                }

                // 3. Удаляем пользователя
                String sql3 = "DELETE FROM AppUser WHERE username = ?";
                int deletedRows;
                try (PreparedStatement ps = conn.prepareStatement(sql3)) {
                    ps.setString(1, username);
                    deletedRows = ps.executeUpdate();
                }

                conn.commit();

                if (deletedRows > 0) {
                    return new Response(true, "Пользователь " + username + " успешно удалён", null);
                } else {
                    return new Response(false, "Пользователь не найден", null);
                }

            } catch (Exception e) {
                conn.rollback();
                return new Response(false, "Ошибка при удалении пользователя: " + e.getMessage(), null);
            }
        } catch (SQLException e) {
            return new Response(false, "Ошибка подключения к БД: " + e.getMessage(), null);
        }
    }
    private Response handleOptimize(Request req) {
        if (!isAuthorized(req.token, null)) return new Response(false, "Нужна авторизация", null);
        if (!(req.data instanceof PortfolioRequest pReq)) return new Response(false, "Ошибка данных", null);

        try {
            PortfolioOptimizer optimizer = OptimizerFactory.getOptimizer();
            PortfolioResult result = optimizer.optimize(pReq.assets, pReq.targetRisk);
            return new Response(true, "Оптимизация выполнена", result);
        } catch (Exception e) {
            return new Response(false, "Ошибка оптимизации: " + e.getMessage(), null);
        }
    }
    private Response handleUpdateAssetDB(Request req) {
        // 1. Проверка авторизации и формата данных
        if (!isAuthorized(req.token, "ADMIN")) return new Response(false, "Доступ запрещен", null);
        if (!(req.data instanceof Map<?, ?>)) return new Response(false, "Неверный формат данных", null);

        Map<String, String> data = (Map<String, String>) req.data;

        // Извлекаем все поля, которые прислал клиент
        String symbol = data.get("symbol");
        String name = data.get("name");
        String priceStr = data.get("current_price");
        String covStr = data.get("covariance_value");

        if (symbol == null || symbol.isEmpty()) return new Response(false, "Ошибка: Symbol не указан", null);

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); // Включаем транзакцию для работы с двумя таблицами

            // 2. Проверяем, существует ли уже такой актив в таблице cryptoasset
            String checkSql = "SELECT id FROM cryptoasset WHERE symbol = ?";
            int assetId = -1;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, symbol);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) assetId = rs.getInt("id");
            }

            if (assetId == -1) {
                // 3. ЕСЛИ НЕТ: INSERT в таблицу cryptoasset
                // Используем только те колонки, которые реально есть в этой таблице
                String insertAsset = "INSERT INTO cryptoasset (symbol, name, current_price) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertAsset, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, symbol);
                    ps.setString(2, (name != null && !name.isEmpty()) ? name : symbol);
                    ps.setDouble(3, (priceStr != null && !priceStr.isEmpty()) ? Double.parseDouble(priceStr) : 0.0);
                    ps.executeUpdate();

                    ResultSet gk = ps.getGeneratedKeys();
                    if (gk.next()) assetId = gk.getInt(1); // Получаем новый ID
                }

                // 4. INSERT в таблицу assetcovariance
                // Обязательно заполняем asset_b_id тем же ID, чтобы не было ошибки default value
                String insertCov = "INSERT INTO assetcovariance (asset_a_id, asset_b_id, covariance_value) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertCov)) {
                    ps.setInt(1, assetId);
                    ps.setInt(2, assetId);
                    ps.setDouble(3, (covStr != null && !covStr.isEmpty()) ? Double.parseDouble(covStr) : 0.05);
                    ps.executeUpdate();
                }
            } else {
                // 5. ЕСЛИ ЕСТЬ: UPDATE существующих записей
                String updateAsset = "UPDATE cryptoasset SET name = ?, current_price = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateAsset)) {
                    ps.setString(1, (name != null && !name.isEmpty()) ? name : symbol);
                    ps.setDouble(2, (priceStr != null && !priceStr.isEmpty()) ? Double.parseDouble(priceStr) : 0.0);
                    ps.setInt(3, assetId);
                    ps.executeUpdate();
                }

                // Обновляем риск (связь актива с самим собой) в таблице ковариаций
                String updateCov = "UPDATE assetcovariance SET covariance_value = ? WHERE asset_a_id = ? AND asset_b_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateCov)) {
                    ps.setDouble(1, (covStr != null && !covStr.isEmpty()) ? Double.parseDouble(covStr) : 0.05);
                    ps.setInt(2, assetId);
                    ps.setInt(3, assetId);
                    ps.executeUpdate();
                }
            }

            conn.commit(); // Сохраняем все изменения в БД
            return new Response(true, "Актив " + symbol + " успешно сохранен (БД обновлена)", null);

        } catch (Exception e) {
            // В случае ошибки (например, NullPointerException или SQLException) откатываем изменения
            return new Response(false, "Ошибка БД: " + e.getMessage(), null);
        }
    }
    private Response handleDeleteAsset(Request req) {
        if (!isAuthorized(req.token, "ADMIN")) return new Response(false, "Доступ запрещен", null);
        String symbol = (String) req.data;

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            // 1. Сначала находим ID актива по символу
            String findIdSql = "SELECT id FROM cryptoasset WHERE symbol = ?";
            int assetId = -1;
            try (PreparedStatement ps = conn.prepareStatement(findIdSql)) {
                ps.setString(1, symbol);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) assetId = rs.getInt("id");
            }

            if (assetId == -1) return new Response(false, "Актив не найден в БД", null);

            // 2. Удаляем все связи из assetcovariance
            String delCov = "DELETE FROM assetcovariance WHERE asset_a_id = ? OR asset_b_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(delCov)) {
                ps.setInt(1, assetId);
                ps.setInt(2, assetId);
                ps.executeUpdate();
            }

            // 3. Удаляем сам актив из cryptoasset
            String delAsset = "DELETE FROM cryptoasset WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(delAsset)) {
                ps.setInt(1, assetId);
                ps.executeUpdate();
            }

            conn.commit();
            return new Response(true, "Актив " + symbol + " полностью удален", null);
        } catch (Exception e) {
            return new Response(false, "Ошибка удаления: " + e.getMessage(), null);
        }
    }
    private Response handleSavePortfolio(Request req) {
        if (!isAuthorized(req.token, null))
            return new Response(false, "Неавторизован", null);

        if (!(req.data instanceof Map<?, ?> data))
            return new Response(false, "Неверный формат данных", null);

        String name = (String) data.get("name");
        Double targetRisk = (Double) data.get("targetRisk");
        @SuppressWarnings("unchecked")
        Map<String, Double> weights = (Map<String, Double>) data.get("weights");

        if (name == null || targetRisk == null || weights == null || weights.isEmpty())
            return new Response(false, "Отсутствуют обязательные поля (name, targetRisk, weights)", null);

        int userId = getUserIdByToken(req.token);
        if (userId == -1)
            return new Response(false, "Пользователь не найден", null);

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            // 1. Создаём запись в portfolio
            String insertPortfolio = "INSERT INTO portfolio (user_id, name, target_risk) VALUES (?, ?, ?)";
            int portfolioId;
            try (PreparedStatement ps = conn.prepareStatement(insertPortfolio, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setString(2, name);
                ps.setDouble(3, targetRisk);
                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    portfolioId = keys.getInt(1);
                } else {
                    throw new SQLException("Не удалось получить ID портфеля");
                }
            }

            // 2. Получаем asset_id по symbol и сохраняем состав
            String insertComp = "INSERT INTO portfoliocomposition (portfolio_id, asset_id, asset_weight) " +
                    "VALUES (?, (SELECT id FROM cryptoasset WHERE symbol = ?), ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertComp)) {
                for (var entry : weights.entrySet()) {
                    String symbol = entry.getKey().toUpperCase();
                    double weight = entry.getValue();

                    ps.setInt(1, portfolioId);
                    ps.setString(2, symbol);
                    ps.setDouble(3, weight);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return new Response(true, "Портфель успешно сохранён", Map.of("portfolioId", portfolioId));

        } catch (Exception e) {
            return new Response(false, "Ошибка сохранения портфеля: " + e.getMessage(), null);
        }
    }

    private Response handleGetPortfolioHistory(Request req) {
        if (!isAuthorized(req.token, null))
            return new Response(false, "Неавторизован", null);

        int userId = getUserIdByToken(req.token);
        if (userId == -1)
            return new Response(false, "Пользователь не найден", null);

        String sql = """
            SELECT p.id, p.name, p.target_risk, p.created_at,
                   GROUP_CONCAT(CONCAT(c.symbol, ':', pc.asset_weight) SEPARATOR '; ') AS composition
            FROM portfolio p
            JOIN portfoliocomposition pc ON p.id = pc.portfolio_id
            JOIN cryptoasset c ON pc.asset_id = c.id
            WHERE p.user_id = ?
            GROUP BY p.id, p.name, p.target_risk, p.created_at
            ORDER BY p.created_at DESC
            """;

        List<Map<String, Object>> history = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("id", rs.getInt("id"));
                    entry.put("name", rs.getString("name"));
                    entry.put("targetRisk", rs.getDouble("target_risk"));
                    entry.put("date", rs.getTimestamp("created_at").toString());
                    entry.put("composition", rs.getString("composition"));
                    history.add(entry);
                }
            }
            return new Response(true, "История портфелей получена", history);
        } catch (SQLException e) {
            return new Response(false, "Ошибка БД: " + e.getMessage(), null);
        }
    }
    private Response handleExportReport(Request req) {
        if (!isAuthorized(req.token, null))
            return new Response(false, "Неавторизован", null);

        if (!(req.data instanceof Map<?, ?> data))
            return new Response(false, "Неверный формат данных", null);

        Map<String, Double> weights = new HashMap<>();

        // Режим 1: экспорт по portfolioId (из истории)
        if (data.containsKey("portfolioId")) {
            Integer portfolioId = (Integer) data.get("portfolioId");
            if (portfolioId == null)
                return new Response(false, "portfolioId не указан", null);

            weights = loadWeightsByPortfolioId(portfolioId);
            if (weights.isEmpty())
                return new Response(false, "Портфель не найден или пустой", null);
        }
        // Режим 2: экспорт текущих весов (после OPTIMIZE)
        else if (data.containsKey("weights")) {
            @SuppressWarnings("unchecked")
            Map<String, Double> w = (Map<String, Double>) data.get("weights");
            weights = w;
        }
        else {
            return new Response(false, "Укажите portfolioId или weights", null);
        }

        try {
            PortfolioService.exportToCSV(weights);
            return new Response(true,
                    "Отчёт успешно экспортирован в portfolio_report.csv",
                    "Файл сохранён на сервере (корень проекта)");
        } catch (Exception e) {
            return new Response(false, "Ошибка экспорта: " + e.getMessage(), null);
        }
    }
    private Map<String, Double> loadWeightsByPortfolioId(int portfolioId) {
        Map<String, Double> weights = new HashMap<>();
        String sql = """
            SELECT c.symbol, pc.asset_weight 
            FROM portfoliocomposition pc
            JOIN cryptoasset c ON pc.asset_id = c.id
            WHERE pc.portfolio_id = ?
            """;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, portfolioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    weights.put(rs.getString("symbol"), rs.getDouble("asset_weight"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка загрузки весов портфеля: " + e.getMessage());
        }
        return weights;
    }
    private int getUserIdByToken(String token) {
        String username = Server.activeTokens.get(token);
        if (username == null) return -1;

        String sql = "SELECT id FROM AppUser WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException ignored) {}
        return -1;
    }
    private boolean isAuthorized(String token, String requiredRole) {
        if (token == null || !Server.activeTokens.containsKey(token)) return false;
        if (requiredRole != null) {
            String role = Server.tokenToRole.get(token);
            return requiredRole.equalsIgnoreCase(role);
        }
        return true;
    }
}