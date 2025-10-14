package com.example.demo.enums;

public enum Action {
    OK("ok"),
    WARNING("warning"), 
    ERROR("error");

    private final String code;

    Action(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
