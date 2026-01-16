package com.example.demo.dto;

public record LoginResponse(
        boolean success,
        String role,
        String username
) {
}
