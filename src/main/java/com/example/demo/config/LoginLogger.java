package com.example.demo.config;

import java.util.logging.Logger;

import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import com.example.demo.service.TelegramLogService;

@Component
public class LoginLogger implements ApplicationListener<AuthenticationSuccessEvent> {

    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_RESET = "\u001B[0m";

    private final TelegramLogService telegram;

    public LoginLogger(TelegramLogService telegram) {
        this.telegram = telegram;
    }

    @Override
    public void onApplicationEvent(@NonNull AuthenticationSuccessEvent event) {
        String userName = event.getAuthentication().getName();

        Logger.getLogger("AuthenticationSuccessEvent")
                .info(ANSI_PURPLE + "User [" + userName + "] logged successfully" + ANSI_RESET);

        telegram.send("LOGIN OK: user=" + userName);
    }
}
