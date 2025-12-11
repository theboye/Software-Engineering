package com.example.config;

import com.example.bot.MyTelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(MyTelegramBot bot) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            System.out.println("=== БОТ УСПЕШНО ЗАРЕГИСТРИРОВАН ===");
            return botsApi;
        } catch (TelegramApiException e) {
            System.err.println("❌ ОШИБКА РЕГИСТРАЦИИ БОТА: " + e.getMessage());
            throw new RuntimeException("Failed to register bot", e);
        }
    }
}