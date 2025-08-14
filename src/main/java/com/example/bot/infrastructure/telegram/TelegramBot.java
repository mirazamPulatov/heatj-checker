package com.example.bot.infrastructure.telegram;

import com.example.bot.application.BotCommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private final String botUsername;
    private final BotCommandHandler commandHandler;

    public TelegramBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            BotCommandHandler commandHandler) {
        super(botToken);
        this.botUsername = botUsername;
        this.commandHandler = commandHandler;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        var message = update.getMessage();
        var text = message.getText();
        var chatId = message.getChatId();

        String[] parts = text.split("\\s+", 2);
        String command = parts[0];
        Optional<String> argument = parts.length > 1 ? Optional.of(parts[1]) : Optional.empty();

        String responseText = commandHandler.handleCommand(chatId, command, argument);
        sendMessage(chatId, responseText);
    }

    public void sendMessage(long chatId, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        var message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send message to chat {}: {}", chatId, e.getMessage(), e);
        }
    }
}
