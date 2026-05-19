package com.example.criptoserver;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    public String command;
    public Object data;
    public String token;        // ← добавлено для JWT

    public Request(String command, Object data) {
        this.command = command;
        this.data = data;
        this.token = null;
    }

    // Удобный конструктор с токеном
    public Request(String command, Object data, String token) {
        this.command = command;
        this.data = data;
        this.token = token;
    }
}