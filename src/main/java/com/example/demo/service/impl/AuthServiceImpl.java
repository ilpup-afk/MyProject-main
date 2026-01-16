package com.example.demo.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserLoggedDto;
import com.example.demo.exception.AppException;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest, String accessToken, String refreshToken) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.username(),
                            loginRequest.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsername(loginRequest.username())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

            log.info("User logged in: {}", user.getUsername());

            LoginResponse response = new LoginResponse(
                    true,
                    user.getRole().getName(),
                    user.getUsername()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Login failed", e);
            throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    @Override
    public ResponseEntity<LoginResponse> refresh(String refreshToken) {
        throw new AppException(HttpStatus.BAD_REQUEST, "Refresh token is invalid");
    }

    @Override
    public ResponseEntity<LoginResponse> logout(String accessToken, String refreshToken) {
        SecurityContextHolder.clearContext();
        
        LoginResponse response = new LoginResponse(false, null, null);
        return ResponseEntity.ok(response);
    }

    @Override
    public UserLoggedDto getUserLoggedInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof AnonymousAuthenticationToken) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "No user authenticated");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        return UserMapper.userToUserLoggedDto(user);
    }
}
