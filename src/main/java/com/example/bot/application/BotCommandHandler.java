package com.example.bot.application;

import com.example.bot.domain.MonitoredService;
import com.example.bot.infrastructure.persistence.MonitoredServiceRepository;
import com.example.bot.infrastructure.system.SystemServiceManager;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class BotCommandHandler {

    private final MonitoredServiceRepository repository;
    private final SystemServiceManager systemServiceManager;

    public BotCommandHandler(MonitoredServiceRepository repository, SystemServiceManager systemServiceManager) {
        this.repository = repository;
        this.systemServiceManager = systemServiceManager;
    }

    public String handleCommand(long chatId, String command, Optional<String> argument) {
        return switch (command) {
            case "/monitor" -> handleMonitor(chatId, argument);
            case "/unmonitor" -> handleUnmonitor(chatId, argument);
            case "/list" -> handleList(chatId);
            case "/status" -> handleStatus(chatId, argument);
            case "/start" -> handleControlCommand("start", argument);
            case "/stop" -> handleControlCommand("stop", argument);
            default -> "Unknown command. Available: /monitor, /unmonitor, /list, /status, /start, /stop.";
        };
    }

    private String handleMonitor(long chatId, Optional<String> serviceNameOpt) {
        return serviceNameOpt.map(serviceName -> {
            if (repository.findByChatIdAndServiceName(chatId, serviceName).isPresent()) {
                return String.format("Service `%s` is already being monitored in this chat.", serviceName);
            }
            repository.save(new MonitoredService(chatId, serviceName));
            return String.format("✅ Started monitoring service: `%s`", serviceName);
        }).orElse("Usage: `/monitor <serviceName>`");
    }

    private String handleUnmonitor(long chatId, Optional<String> serviceNameOpt) {
        return serviceNameOpt.map(serviceName -> {
            if (repository.findByChatIdAndServiceName(chatId, serviceName).isEmpty()) {
                return String.format("Service `%s` is not being monitored in this chat.", serviceName);
            }
            repository.deleteByChatIdAndServiceName(chatId, serviceName);
            return String.format("❌ Stopped monitoring service: `%s`", serviceName);
        }).orElse("Usage: `/unmonitor <serviceName>`");
    }

    private String handleList(long chatId) {
        var services = repository.findByChatId(chatId);
        if (services.isEmpty()) {
            return "No services are being monitored in this chat.";
        }
        var serviceList = services.stream()
                .map(s -> "- `" + s.getServiceName() + "` (Status: " + s.getLastStatus() + ")")
                .collect(Collectors.joining("\n"));
        return "*Monitored Services:*\n" + serviceList;
    }

    private String handleStatus(long chatId, Optional<String> serviceNameOpt) {
        // Note: /status can check any service, not just monitored ones.
        return serviceNameOpt.map(serviceName -> {
            var rawStatus = systemServiceManager.getServiceStatus(serviceName);
            return String.format("*Status for `%s`:*\n```\n%s\n```", serviceName, rawStatus);
        }).orElse("Usage: `/status <serviceName>`");
    }

    private String handleControlCommand(String action, Optional<String> serviceNameOpt) {
        return serviceNameOpt.map(serviceName -> {
            String result;
            if (action.equals("start")) {
                result = systemServiceManager.startService(serviceName);
            } else {
                result = systemServiceManager.stopService(serviceName);
            }
            return String.format("*Result of `%s %s`:*\n```\n%s\n```", action, serviceName, result);
        }).orElse(String.format("Usage: `/%s <serviceName>`", action));
    }
}
