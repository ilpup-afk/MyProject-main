package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserDto;
import com.example.demo.dto.UserLoggedDto;
import com.example.demo.service.UserService;
import com.example.demo.service.impl.AuthServiceImpl;
import lombok.RequiredArgsConstructor;

@Tag(name = "Authentication", description = "API для аутентификации и управления сессиями")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthServiceImpl authService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "Логин пользователя", description = "Аутентификация пользователя по логину и паролю. Возвращает JWT-токены.")    
    @ApiResponse(responseCode = "200", description = "Успешная аутентификация")
    @ApiResponse(responseCode = "500", description = "Неверные учетные данные пользователя")    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @CookieValue(name = "access_token", required = false) 
            String accessToken,
            @CookieValue(name = "refresh_token", required = false) 
            String refreshToken,
            @RequestBody LoginRequest loginRequest) {
        
      
        return authService.login(loginRequest, accessToken, refreshToken);
        
    }

    @Operation(summary = "Обновление токена", description = "Генерация нового access-токена по refresh-токену")
    @ApiResponse(responseCode = "200", description = "Токен успешно обновлен")
    @ApiResponse(responseCode = "400", description = "Недействительный refresh-токен (Refresh token is invalid)")
    @ApiResponse(responseCode = "404", description = "Не был получен токен")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.notFound().build();
        }
        return authService.refresh(refreshToken);
    }

    @Operation(summary = "Выход из системы", description = "Инвалидация текущих JWT-токенов")
    @ApiResponse(responseCode = "200", description = "Сессия завершена")
    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ")
    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(
            @CookieValue(name = "access_token", required = false) String accessToken,
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        return authService.logout(accessToken, refreshToken);
    }

    @Operation(summary = "Информация о пользователе", description = "Получение данных текущего аутентифицированного пользователя", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Данные пользователя")
    @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/info")
    public ResponseEntity<UserLoggedDto> userLoggedInfo() {
        return ResponseEntity.ok(authService.getUserLoggedInfo());
    }

    @PutMapping("/change_password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {

        if (!request.confirmPassword().equals(request.newPassword())) {
            return ResponseEntity.badRequest().build();
        }
        UserDto user = userService.getUser(authService.getUserLoggedInfo().username());
        if (passwordEncoder.matches(request.currentPassword(), 
        user.password())) {
            userService.updateUser(user.id(),
                    new UserDto(user.id(), user.username(),
                            request.newPassword(), user.role(), user.permissions()));
            return ResponseEntity.ok("пароль успешно изменен");
        }
        return ResponseEntity.notFound().build();
    }
}