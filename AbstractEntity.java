package com.example.criptoserver;

import java.io.Serializable;

public abstract class AbstractEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    protected int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public abstract boolean validate();
}