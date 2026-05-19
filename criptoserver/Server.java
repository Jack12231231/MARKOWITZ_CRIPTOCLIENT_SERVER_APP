package com.example.criptoserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 3060;
    private static final int THREAD_POOL_SIZE = 10;

    // Авторизация
    public static final ConcurrentHashMap<String, String> activeTokens = new ConcurrentHashMap<>(); // token -> username
    public static final ConcurrentHashMap<String, String> tokenToRole = new ConcurrentHashMap<>();   // token -> role

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер запущен на порту " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Новое подключение: " + clientSocket.getInetAddress());
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }
}