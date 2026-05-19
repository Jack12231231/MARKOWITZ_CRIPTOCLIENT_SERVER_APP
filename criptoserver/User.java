package com.example.criptoserver;

public class User extends AbstractEntity {
    private String username;
    private String passwordHash;
    private int roleId;

    public User() {}
    public User(String username, String passwordHash, int roleId) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roleId = roleId;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public int getRoleId() { return roleId; }

    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRoleId(int roleId) { this.roleId = roleId; }

    @Override
    public boolean validate() {
        return username != null && !username.trim().isEmpty() && passwordHash != null;
    }
}