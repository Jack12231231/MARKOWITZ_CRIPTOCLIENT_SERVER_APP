package com.example.criptoclient;

import java.io.*;
import java.net.Socket;

public class ClientNetwork {
    private static ClientNetwork instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private ClientNetwork() {}

    public static synchronized ClientNetwork getInstance() {
        if (instance == null) instance = new ClientNetwork();
        return instance;
    }

    public void connect() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket("localhost", 3060);
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());
        }
    }

    public com.example.criptoserver.Response sendRequest(com.example.criptoserver.Request req) {
        try {
            connect();
            out.writeObject(req);
            out.flush();
            out.reset();
            return (com.example.criptoserver.Response) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new com.example.criptoserver.Response(false, "Ошибка связи с сервером: " + e.getMessage(), null);
        }
    }

    public void disconnect() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}