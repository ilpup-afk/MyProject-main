package com.example.demo.service;
import org.springframework.http.ResponseEntity;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserLoggedDto;


public interface AuthService {
    ResponseEntity<LoginResponse> login(LoginRequest loginRequest, String accessToken, String refreshToken);

    ResponseEntity<LoginResponse> refresh(String refreshToken);

    ResponseEntity<LoginResponse> logout(String accessToken, String refreshToken);

    UserLoggedDto getUserLoggedInfo();
}