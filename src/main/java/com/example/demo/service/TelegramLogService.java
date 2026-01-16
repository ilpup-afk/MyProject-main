package com.example.demo.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TelegramLogService {

    private final RestClient.Builder restClientBuilder;

    @Value("${telegram.enabled:false}")
    private boolean enabled;

    @Value("${telegram.bot.token:}")
    private String token;

    @Value("${telegram.chatId:}")
    private String chatId;

    public void send(String text) {
        if (!enabled) return;
        if (token == null || token.isBlank()) return;
        if (chatId == null || chatId.isBlank()) return;
        if (text == null || text.isBlank()) return;

        String url = "https://api.telegram.org/bot" + token + "/sendMessage";

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("parse_mode", "HTML");

        try {
            restClientBuilder.build()
                    .post()
                    .uri(url)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ignored) {

        }
    }
}
