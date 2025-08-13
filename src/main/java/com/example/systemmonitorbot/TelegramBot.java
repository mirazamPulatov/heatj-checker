package com.example.systemmonitorbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;
import java.util.List;

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
        List<BotApiMethod<? extends Serializable>> responses = updateHandler.handleUpdate(update);
        if (responses != null && !responses.isEmpty()) {
            for (BotApiMethod<?> response : responses) {
                try {
                    if (response instanceof SendMessage) {
                        execute((SendMessage) response);
                    } else if (response instanceof EditMessageText) {
                        execute((EditMessageText) response);
                    } else if (response instanceof AnswerCallbackQuery) {
                        execute((AnswerCallbackQuery) response);
                    }
                } catch (TelegramApiException e) {
                    logger.error("Failed to execute response: {}", e.getMessage());
                }
            }
        }
    }

    public void sendAlertToChannel(String text) {
        sendMessage(this.channelId, text);
    }

    public void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        try {
            execute(message);
            logger.info("Sent message to chat {}: {}", chatId, text);
        } catch (TelegramApiException e) {
            logger.error("Failed to send message to chat {}: {}", chatId, e.getMessage());
        }
    }

    public void sendMessage(long chatId, String text) {
        sendMessage(String.valueOf(chatId), text);
    }
}
