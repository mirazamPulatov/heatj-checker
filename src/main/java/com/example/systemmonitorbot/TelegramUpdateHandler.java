package com.example.systemmonitorbot;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TelegramUpdateHandler {

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

    private List<BotApiMethod<? extends Serializable>> handleCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] data = callbackQuery.getData().split(":", 2);
        String action = data[0];
        String serviceName = data[1];
        long chatId = callbackQuery.getMessage().getChatId();
        int messageId = callbackQuery.getMessage().getMessageId();

        String resultText;
        switch (action) {
            case "status":
                resultText = systemServiceManager.getRawServiceStatus(serviceName);
                break;
            case "start":
                systemServiceManager.startService(serviceName);
                resultText = "Start command issued for `" + serviceName + "`. Checking status...";
                // Add a small delay to allow the service to start before checking status
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                resultText += "\n\n" + systemServiceManager.getRawServiceStatus(serviceName);
                break;
            case "stop":
                systemServiceManager.stopService(serviceName);
                resultText = "Stop command issued for `" + serviceName + "`. Checking status...";
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                resultText += "\n\n" + systemServiceManager.getRawServiceStatus(serviceName);
                break;
            default:
                resultText = "Unknown action.";
        }

        List<BotApiMethod<? extends Serializable>> responses = new ArrayList<>();

        // 1. Answer the callback query to remove the "loading" state on the button
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQuery.getId());
        answer.setText("Action: " + action + " on " + serviceName);
        responses.add(answer);

        // 2. Edit the original message with the result
        EditMessageText editedMessage = new EditMessageText();
        editedMessage.setChatId(String.valueOf(chatId));
        editedMessage.setMessageId(messageId);
        editedMessage.setText("*Result for " + action.toUpperCase() + " on `" + serviceName + "`*\n```\n" + resultText + "\n```");
        editedMessage.setParseMode("Markdown");
        // We can optionally re-add the keyboard if we want the user to perform more actions
        editedMessage.setReplyMarkup(createManagementKeyboard(serviceName));
        responses.add(editedMessage);

        return responses;
    }

    private List<BotApiMethod<? extends Serializable>> handleMessageUpdate(Update update) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        String[] parts = messageText.split("\\s+");
        String command = parts[0];
        String argument = parts.length > 1 ? parts[1] : null;

        switch (command) {
            case "/start":
                return Collections.singletonList(createMessage(chatId, "Welcome! Use /monitor <service> to start monitoring or /manage to see your services."));
            case "/monitor":
                return Collections.singletonList(handleMonitor(chatId, argument));
            case "/unmonitor":
                return Collections.singletonList(handleUnmonitor(chatId, argument));
            case "/list":
                return Collections.singletonList(handleList(chatId));
            case "/status":
                return Collections.singletonList(handleStatus(chatId, argument));
            case "/manage":
                return handleManage(chatId);
            default:
                return Collections.singletonList(createMessage(chatId, "Unknown command. Try /help."));
        }
    }

    private SendMessage handleMonitor(long chatId, String serviceName) {
        if (serviceName == null) {
            return createMessage(chatId, "Please specify a service name. Usage: `/monitor <serviceName>`");
        }
        if (repository.findByChatIdAndServiceName(chatId, serviceName).isPresent()) {
            return createMessage(chatId, "Service `" + serviceName + "` is already being monitored.");
        }
        MonitoredService service = new MonitoredService(chatId, serviceName);
        repository.save(service);
        return createMessage(chatId, "✅ Started monitoring service: `" + serviceName + "`");
    }

    private SendMessage handleUnmonitor(long chatId, String serviceName) {
        if (serviceName == null) {
            return createMessage(chatId, "Please specify a service name. Usage: `/unmonitor <serviceName>`");
        }
        if (repository.findByChatIdAndServiceName(chatId, serviceName).isEmpty()) {
            return createMessage(chatId, "Service `" + serviceName + "` is not being monitored.");
        }
        repository.deleteByChatIdAndServiceName(chatId, serviceName);
        return createMessage(chatId, "❌ Stopped monitoring service: `" + serviceName + "`");
    }

    private SendMessage handleList(long chatId) {
        List<MonitoredService> services = repository.findByChatId(chatId);
        if (services.isEmpty()) {
            return createMessage(chatId, "You are not monitoring any services.");
        }
        String serviceList = services.stream()
                .map(s -> "- `" + s.getServiceName() + "`")
                .collect(Collectors.joining("\n"));
        return createMessage(chatId, "*Monitored Services:*\n" + serviceList);
    }

    private SendMessage handleStatus(long chatId, String serviceName) {
        if (serviceName == null) {
            return createMessage(chatId, "Please specify a service name. Usage: `/status <serviceName>`");
        }
        String rawStatus = systemServiceManager.getRawServiceStatus(serviceName);
        String messageText = "*Status for `" + serviceName + "`:*\n```\n" + rawStatus + "\n```";
        return createMessage(chatId, messageText);
    }

    private List<BotApiMethod<? extends Serializable>> handleManage(long chatId) {
        List<MonitoredService> services = repository.findByChatId(chatId);
        if (services.isEmpty()) {
            return Collections.singletonList(createMessage(chatId, "You are not monitoring any services. Use `/monitor <serviceName>` to add one."));
        }

        List<BotApiMethod<? extends Serializable>> messages = new ArrayList<>();
        for (MonitoredService service : services) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Managing service: `" + service.getServiceName() + "`");
            message.setParseMode("Markdown");
            message.setReplyMarkup(createManagementKeyboard(service.getServiceName()));
            messages.add(message);
        }
        return messages;
    }

    private InlineKeyboardMarkup createManagementKeyboard(String serviceName) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton statusButton = new InlineKeyboardButton();
        statusButton.setText("Status");
        statusButton.setCallbackData("status:" + serviceName);

        InlineKeyboardButton startButton = new InlineKeyboardButton();
        startButton.setText("Start");
        startButton.setCallbackData("start:" + serviceName);

        InlineKeyboardButton stopButton = new InlineKeyboardButton();
        stopButton.setText("Stop");
        stopButton.setCallbackData("stop:" + serviceName);

        row.add(statusButton);
        row.add(startButton);
        row.add(stopButton);
        keyboard.add(row);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private SendMessage createMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        return message;
    }
}
