package com.example.systemmonitorbot;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

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

    public SendMessage handleUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            String[] parts = messageText.split("\\s+");
            String command = parts[0];
            String argument = parts.length > 1 ? parts[1] : null;

            switch (command) {
                case "/start":
                    return createMessage(chatId, "Welcome! Use /monitor <service> to start monitoring.");
                case "/monitor":
                    return handleMonitor(chatId, argument);
                case "/unmonitor":
                    return handleUnmonitor(chatId, argument);
                case "/list":
                    return handleList(chatId);
                case "/status":
                    return handleStatus(chatId, argument);
                default:
                    return createMessage(chatId, "Unknown command. Try /help.");
            }
        }
        return null;
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

    private SendMessage createMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        return message;
    }
}
