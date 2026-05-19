package com.example.criptoserver;

import java.io.Serializable;

public class LoginData implements Serializable {
    private static final long serialVersionUID = 1L;

    public String username;
    public String password;

    public LoginData(String username, String password) {
        this.username = username;
        this.password = password;
    }
}