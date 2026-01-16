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

import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserDto;
import com.example.demo.dto.UserLoggedDto;
import com.example.demo.service.UserService;
import com.example.demo.service.impl.AuthServiceImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(
    name = "auth-controller",
    description = """
        API для аутентификации и управления сессиями (JWT + cookies).
        """
)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthServiceImpl authService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Operation(
        summary = "Логин пользователя",
        description = """
            Выполняет аутентификацию пользователя и возвращает информацию о результате входа.

            ### Процесс:
            1) Читает cookies access_token / refresh_token (если переданы)
            2) Проверяет логин/пароль
            3) Генерирует/обновляет JWT токены (access + refresh) и выставляет их в cookies
            4) Возвращает LoginResponse

            ### Требования:
            - Для нового входа нужен request body с username/password.
            - Cookies могут быть переданы, если клиент уже логинился ранее.
            """,
        tags = {"auth-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Успешная аутентификация, токены обновлены/установлены"),
        @ApiResponse(responseCode = "400", description = "Некорректный запрос (например, неверные входные данные)"),
        @ApiResponse(responseCode = "401", description = "Неверные учетные данные / неуспешная аутентификация"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @CookieValue(name = "access_token", required = false) String accessToken,
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            @RequestBody LoginRequest loginRequest) {

        return authService.login(loginRequest, accessToken, refreshToken);
    }

    @Operation(
        summary = "Обновление токенов (refresh)",
        description = """
            Обновляет access token (и/или пару access+refresh) на основании refresh_token.

            ### Когда использовать:
            - Когда access_token истёк (клиент получает 401)
            - Для продления сессии без повторного ввода логина/пароля

            ### Важно:
            - refresh_token должен быть передан в cookie refresh_token
            """,
        tags = {"auth-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Токены успешно обновлены"),
        @ApiResponse(responseCode = "400", description = "Refresh token is invalid / неверный refresh_token"),
        @ApiResponse(responseCode = "404", description = "refresh_token cookie отсутствует"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        if (refreshToken == null) {
            return ResponseEntity.notFound().build();
        }
        return authService.refresh(refreshToken);
    }

    @Operation(
        summary = "Выход из системы (logout)",
        description = """
            Завершает пользовательскую сессию.

            ### Процесс:
            1) Инвалидирует/отзывает токены (логика зависит от AuthServiceImpl)
            2) Очищает SecurityContext (если используется)
            3) Возвращает LoginResponse(success=false)

            ### Примечание:
            - access_token и refresh_token читаются из cookies
            """,
        tags = {"auth-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Сессия завершена"),
        @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(
            @CookieValue(name = "access_token", required = false) String accessToken,
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        return authService.logout(accessToken, refreshToken);
    }

    @Operation(
        summary = "Информация о текущем пользователе",
        description = """
            Возвращает данные текущего аутентифицированного пользователя.

            ### Требования:
            - Пользователь должен быть аутентифицирован (иначе 401/403)
            - Данные берутся из SecurityContext (внутри AuthServiceImpl.getUserLoggedInfo())

            ### Когда использовать:
            - Проверка текущей роли/прав
            - Отображение профиля пользователя
            """,
        security = @SecurityRequirement(name = "bearerAuth"),
        tags = {"auth-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Данные пользователя получены"),
        @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/info")
    public ResponseEntity<UserLoggedDto> userLoggedInfo() {
        return ResponseEntity.ok(authService.getUserLoggedInfo());
    }

    @Operation(
        summary = "Сменить пароль",
        description = """
            Меняет пароль текущего пользователя.

            ### Процесс:
            1) Проверяет совпадение newPassword и confirmPassword
            2) Получает текущего пользователя по данным из сессии
            3) Проверяет currentPassword через PasswordEncoder.matches(...)
            4) Сохраняет новый пароль

            ### Ответы:
            - 200 если пароль изменён
            - 400 если confirmPassword != newPassword
            - 404 если пользователь не найден или currentPassword неверный (как в текущей реализации)
            """,
        security = @SecurityRequirement(name = "bearerAuth"),
        tags = {"auth-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Пароль успешно изменён"),
        @ApiResponse(responseCode = "400", description = "Пароли не совпадают (confirmPassword != newPassword)"),
        @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден или неверный текущий пароль"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/change_password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {

        if (!request.confirmPassword().equals(request.newPassword())) {
            return ResponseEntity.badRequest().build();
        }

        UserDto user = userService.getUser(authService.getUserLoggedInfo().username());

        if (passwordEncoder.matches(request.currentPassword(), user.password())) {
            userService.updateUser(
                    user.id(),
                    new UserDto(
                            user.id(),
                            user.username(),
                            request.newPassword(),
                            user.role(),
                            user.permissions()
                    )
            );
            return ResponseEntity.ok("пароль успешно изменен");
        }

        return ResponseEntity.notFound().build();
    }
}
