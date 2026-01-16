package com.example.demo.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TelegramLogService {

    private final RestClient restClient;

    @Value("${telegram.enabled:false}")
    private boolean enabled;

    @Value("${telegram.bot.token:}")
    private String token;

    // ВАЖНО: если в yml ключ telegram.chatId, то здесь telegram.chatId
    // Если хочешь telegram.chat.id — тогда в yml делай telegram: chat: { id: ... }
    @Value("${telegram.chatId:}")
    private String chatId;

    public TelegramLogService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public void send(String text) {
        if (!enabled) return;
        if (token == null || token.isBlank()) return;
        if (chatId == null || chatId.isBlank()) return;
        if (text == null || text.isBlank()) return;

        String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);

        String url = "https://api.telegram.org/bot" + token
                + "/sendMessage?chat_id=" + chatId
                + "&text=" + encoded;

        try {
            restClient.get().uri(url).retrieve().toBodilessEntity();
        } catch (Exception ignored) {
            // Telegram не должен ломать приложение
        }
    }
}
