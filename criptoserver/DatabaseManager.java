package com.example.criptoserver;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
    private static DatabaseManager instance;
    private static final Properties config = new Properties();

    private DatabaseManager() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("com.example.criptoserver/config.properties")) {
            if (is == null) throw new RuntimeException("config.properties не найден!");
            config.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки конфига", e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                config.getProperty("db.url"),
                config.getProperty("db.user"),
                config.getProperty("db.password")
        );
    }
}