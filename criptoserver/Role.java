package com.example.criptoserver;

public class Role extends AbstractEntity {
    private String nameRole;

    public Role() {}
    public Role(String nameRole) {
        this.nameRole = nameRole;
    }

    public String getNameRole() {
        return nameRole;
    }

    public void setNameRole(String nameRole) {
        this.nameRole = nameRole;
    }

    @Override
    public boolean validate() {
        return nameRole != null && !nameRole.trim().isEmpty();
    }
}