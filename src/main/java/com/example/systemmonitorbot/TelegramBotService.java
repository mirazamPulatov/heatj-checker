package com.example.systemmonitorbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private final String botUsername;
    private final MonitoredServiceRepository monitoredServiceRepository;
    private final SystemServiceManager systemServiceManager;

    public TelegramBotService(
            @Value("${bot.token}") String botToken,
            @Value("${bot.username}") String botUsername,
            MonitoredServiceRepository monitoredServiceRepository,
            SystemServiceManager systemServiceManager) {
        super(botToken);
        this.botUsername = botUsername;
        this.monitoredServiceRepository = monitoredServiceRepository;
        this.systemServiceManager = systemServiceManager;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            String[] commandParts = messageText.split(" ");
            String command = commandParts[0];

            switch (command) {
                case "/monitor":
                    handleMonitorCommand(chatId, commandParts);
                    break;
                case "/unmonitor":
                    handleUnmonitorCommand(chatId, commandParts);
                    break;
                case "/status":
                    handleStatusCommand(chatId, commandParts);
                    break;
                case "/list":
                    handleListCommand(chatId);
                    break;
                default:
                    sendMessage(chatId, "Unknown command. Use /monitor, /unmonitor, /status, or /list.");
            }
        }
    }

    private void handleMonitorCommand(long chatId, String[] commandParts) {
        if (commandParts.length < 2) {
            sendMessage(chatId, "Usage: /monitor <service_name>");
            return;
        }
        String serviceName = commandParts[1];
        if (monitoredServiceRepository.findByChatIdAndServiceName(chatId, serviceName).isPresent()) {
            sendMessage(chatId, "Service " + serviceName + " is already being monitored.");
        } else {
            MonitoredService service = new MonitoredService(chatId, serviceName);
            monitoredServiceRepository.save(service);
            sendMessage(chatId, "Started monitoring service: " + serviceName);
        }
    }

    private void handleUnmonitorCommand(long chatId, String[] commandParts) {
        if (commandParts.length < 2) {
            sendMessage(chatId, "Usage: /unmonitor <service_name>");
            return;
        }
        String serviceName = commandParts[1];
        monitoredServiceRepository.findByChatIdAndServiceName(chatId, serviceName).ifPresentOrElse(
                service -> {
                    monitoredServiceRepository.delete(service);
                    sendMessage(chatId, "Stopped monitoring service: " + serviceName);
                },
                () -> sendMessage(chatId, "Service " + serviceName + " is not being monitored.")
        );
    }

    private void handleStatusCommand(long chatId, String[] commandParts) {
        if (commandParts.length < 2) {
            sendMessage(chatId, "Usage: /status <service_name>");
            return;
        }
        String serviceName = commandParts[1];
        String status = systemServiceManager.getServiceStatus(serviceName);
        sendMessage(chatId, "Status of " + serviceName + ": " + status);
    }

    private void handleListCommand(long chatId) {
        List<MonitoredService> services = monitoredServiceRepository.findByChatId(chatId);
        if (services.isEmpty()) {
            sendMessage(chatId, "No services are being monitored in this chat.");
        } else {
            String serviceList = services.stream()
                    .map(MonitoredService::getServiceName)
                    .collect(Collectors.joining("\n"));
            sendMessage(chatId, "Monitored services:\n" + serviceList);
        }
    }

    public void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
