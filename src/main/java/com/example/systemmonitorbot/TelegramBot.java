package com.example.systemmonitorbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private final String botUsername;
    private final String channelId;
    private final TelegramUpdateHandler updateHandler;

    public TelegramBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.channel.id}") String channelId,
            TelegramUpdateHandler updateHandler) {
        super(botToken);
        this.botUsername = botUsername;
        this.channelId = channelId;
        this.updateHandler = updateHandler;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        SendMessage response = updateHandler.handleUpdate(update);
        if (response != null) {
            try {
                execute(response);
            } catch (TelegramApiException e) {
                logger.error("Failed to send message: {}", e.getMessage());
            }
        }
    }

    public void sendAlertToChannel(String text) {
        SendMessage message = new SendMessage();
        message.setChatId(this.channelId);
        message.setText(text);
        message.setParseMode("Markdown");
        try {
            execute(message);
            logger.info("Sent alert to channel {}: {}", this.channelId, text);
        } catch (TelegramApiException e) {
            logger.error("Failed to send alert to channel {}: {}", this.channelId, e.getMessage());
        }
    }
}
