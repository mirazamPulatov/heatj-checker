package com.example.systemmonitorbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TelegramUpdateHandler {

    private static final Logger logger = LoggerFactory.getLogger(TelegramUpdateHandler.class);
    private static final String MARKDOWN = "Markdown";

    private final MonitoredServiceRepository repository;
    private final SystemServiceManager systemServiceManager;

    public TelegramUpdateHandler(MonitoredServiceRepository repository, SystemServiceManager systemServiceManager) {
        this.repository = repository;
        this.systemServiceManager = systemServiceManager;
    }

    public List<BotApiMethod<? extends Serializable>> handleUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            return handleMessageUpdate(update);
        } else if (update.hasCallbackQuery()) {
            return handleCallbackQuery(update);
        }
        return Collections.emptyList();
    }

    private List<BotApiMethod<? extends Serializable>> handleMessageUpdate(Update update) {
        var message = update.getMessage();
        var chatId = message.getChatId();
        String[] parts = message.getText().split("\\s+");
        String command = parts[0];
        Optional<String> argument = parts.length > 1 ? Optional.of(parts[1]) : Optional.empty();

        return switch (command) {
            case "/start" -> Collections.singletonList(createMessage(chatId, "Welcome! Use /monitor <service> to start, or /manage to see your services."));
            case "/monitor" -> Collections.singletonList(handleMonitor(chatId, argument));
            case "/unmonitor" -> Collections.singletonList(handleUnmonitor(chatId, argument));
            case "/list" -> Collections.singletonList(handleList(chatId));
            case "/status" -> Collections.singletonList(handleStatus(chatId, argument));
            case "/manage" -> handleManage(chatId);
            default -> Collections.singletonList(createMessage(chatId, "Unknown command."));
        };
    }

    private List<BotApiMethod<? extends Serializable>> handleCallbackQuery(Update update) {
        var callbackQuery = update.getCallbackQuery();
        String[] data = callbackQuery.getData().split(":", 2);
        if (data.length < 2) {
            return Collections.singletonList(answerCallback(callbackQuery, "Error: Invalid callback data."));
        }

        var action = data[0];
        var serviceName = data[1];
        var chatId = callbackQuery.getMessage().getChatId();
        var messageId = callbackQuery.getMessage().getMessageId();

        var resultText = switch (action) {
            case "status" -> systemServiceManager.getRawServiceStatus(serviceName);
            case "start" -> {
                systemServiceManager.startService(serviceName);
                yield "Start command issued for `" + serviceName + "`. Polling for status...\n\n" +
                      pollForStatus(serviceName, "running", 5, Duration.ofSeconds(1));
            }
            case "stop" -> {
                systemServiceManager.stopService(serviceName);
                yield "Stop command issued for `" + serviceName + "`. Polling for status...\n\n" +
                      pollForStatus(serviceName, "down", 5, Duration.ofSeconds(1));
            }
            default -> "Unknown action.";
        };

        var answer = answerCallback(callbackQuery, "Action: " + action + " on " + serviceName);
        var editedMessage = editMessageWithResult(chatId, messageId, action, serviceName, resultText);

        return List.of(answer, editedMessage);
    }

    private String pollForStatus(String serviceName, String expectedStatus, int maxAttempts, Duration delay) {
        for (int i = 0; i < maxAttempts; i++) {
            try {
                var currentStatus = systemServiceManager.getServiceStatus(serviceName);
                if (currentStatus.equals(expectedStatus)) {
                    break;
                }
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Polling was interrupted.";
            }
        }
        return systemServiceManager.getRawServiceStatus(serviceName);
    }

    private EditMessageText editMessageWithResult(long chatId, int messageId, String action, String serviceName, String resultText) {
        var editedMessage = new EditMessageText();
        editedMessage.setChatId(chatId);
        editedMessage.setMessageId(messageId);
        editedMessage.setText(String.format("""
                *Result for %S on `%s`*
                ```
                %s
                ```
                """, action, serviceName, resultText));
        editedMessage.setParseMode(MARKDOWN);
        editedMessage.setReplyMarkup(createManagementKeyboard(serviceName));
        return editedMessage;
    }

    private AnswerCallbackQuery answerCallback(CallbackQuery callbackQuery, String text) {
        var answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQuery.getId());
        answer.setText(text);
        return answer;
    }

    private SendMessage handleMonitor(long chatId, Optional<String> serviceNameOpt) {
        return serviceNameOpt.map(serviceName -> {
            if (repository.findByChatIdAndServiceName(chatId, serviceName).isPresent()) {
                return createMessage(chatId, "Service `" + serviceName + "` is already monitored.");
            }
            repository.save(new MonitoredService(chatId, serviceName));
            return createMessage(chatId, "✅ Started monitoring service: `" + serviceName + "`");
        }).orElse(createMessage(chatId, "Usage: `/monitor <serviceName>`"));
    }

    private SendMessage handleUnmonitor(long chatId, Optional<String> serviceNameOpt) {
        return serviceNameOpt.map(serviceName -> {
            if (repository.findByChatIdAndServiceName(chatId, serviceName).isEmpty()) {
                return createMessage(chatId, "Service `" + serviceName + "` is not monitored.");
            }
            repository.deleteByChatIdAndServiceName(chatId, serviceName);
            return createMessage(chatId, "❌ Stopped monitoring service: `" + serviceName + "`");
        }).orElse(createMessage(chatId, "Usage: `/unmonitor <serviceName>`"));
    }

    private SendMessage handleList(long chatId) {
        var services = repository.findByChatId(chatId);
        if (services.isEmpty()) {
            return createMessage(chatId, "You are not monitoring any services.");
        }
        var serviceList = services.stream()
                .map(s -> "- `" + s.getServiceName() + "`")
                .collect(Collectors.joining("\n"));
        return createMessage(chatId, "*Monitored Services:*\n" + serviceList);
    }

    private SendMessage handleStatus(long chatId, Optional<String> serviceNameOpt) {
        return serviceNameOpt.map(serviceName -> {
            var rawStatus = systemServiceManager.getRawServiceStatus(serviceName);
            var messageText = String.format("""
                    *Status for `%s`:*
                    ```
                    %s
                    ```
                    """, serviceName, rawStatus);
            return createMessage(chatId, messageText);
        }).orElse(createMessage(chatId, "Usage: `/status <serviceName>`"));
    }

    private List<BotApiMethod<? extends Serializable>> handleManage(long chatId) {
        var services = repository.findByChatId(chatId);
        if (services.isEmpty()) {
            return Collections.singletonList(createMessage(chatId, "You have no services to manage."));
        }
        return services.stream()
                .map(service -> {
                    var message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Managing service: `" + service.getServiceName() + "`");
                    message.setParseMode(MARKDOWN);
                    message.setReplyMarkup(createManagementKeyboard(service.getServiceName()));
                    return (BotApiMethod<? extends Serializable>) message;
                })
                .collect(Collectors.toList());
    }

    private InlineKeyboardMarkup createManagementKeyboard(String serviceName) {
        var statusButton = InlineKeyboardButton.builder().text("Status").callbackData("status:" + serviceName).build();
        var startButton = InlineKeyboardButton.builder().text("Start").callbackData("start:" + serviceName).build();
        var stopButton = InlineKeyboardButton.builder().text("Stop").callbackData("stop:" + serviceName).build();

        var row = List.of(statusButton, startButton, stopButton);
        return InlineKeyboardMarkup.builder().keyboardRow(row).build();
    }

    private SendMessage createMessage(long chatId, String text) {
        var message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode(MARKDOWN);
        return message;
    }
}
