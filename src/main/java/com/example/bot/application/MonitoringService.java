package com.example.bot.application;

import com.example.bot.domain.MonitoredService;
import com.example.bot.infrastructure.persistence.MonitoredServiceRepository;
import com.example.bot.infrastructure.system.SystemServiceManager;
import com.example.bot.infrastructure.telegram.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);
    private static final String RUNNING_IDENTIFIER = "Active: active (running)";
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_DOWN = "down";

    private final MonitoredServiceRepository serviceRepository;
    private final SystemServiceManager systemServiceManager;
    private final TelegramBot telegramBot;

    // Using @Lazy to break circular dependency:
    // MonitoringService -> TelegramBot -> BotCommandHandler -> (potentially back)
    public MonitoringService(
        MonitoredServiceRepository serviceRepository,
        SystemServiceManager systemServiceManager,
        @Lazy TelegramBot telegramBot
    ) {
        this.serviceRepository = serviceRepository;
        this.systemServiceManager = systemServiceManager;
        this.telegramBot = telegramBot;
    }

    @Scheduled(fixedRate = 600000) // 600,000 ms = 10 minutes
    public void checkAllServices() {
        logger.info("Running scheduled health check for all monitored services...");
        var services = serviceRepository.findAll();

        for (var service : services) {
            var output = systemServiceManager.getServiceStatus(service.getServiceName());
            var currentStatus = output.contains(RUNNING_IDENTIFIER) ? STATUS_RUNNING : STATUS_DOWN;
            var lastStatus = service.getLastStatus();

            if (!currentStatus.equalsIgnoreCase(lastStatus)) {
                logger.warn("Status change for service '{}' in chat {}: {} -> {}",
                        service.getServiceName(), service.getChatId(), lastStatus, currentStatus);

                service.setLastStatus(currentStatus);
                serviceRepository.save(service);

                String alertText = switch (currentStatus) {
                    case STATUS_DOWN -> String.format("⚠ Service DOWN: `%s`", service.getServiceName());
                    case STATUS_RUNNING -> String.format("✅ Service UP: `%s`", service.getServiceName());
                    default -> null;
                };

                if (alertText != null) {
                    telegramBot.sendMessage(service.getChatId(), alertText);
                }
            }
        }
        logger.info("Finished scheduled health check.");
    }
}
