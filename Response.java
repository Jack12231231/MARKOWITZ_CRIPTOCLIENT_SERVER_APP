package com.example.criptoserver;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    public boolean success;
    public String message;
    public Object data;

    public Response(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
}