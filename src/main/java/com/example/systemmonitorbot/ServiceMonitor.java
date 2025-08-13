package com.example.systemmonitorbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ServiceMonitor.class);
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_DOWN = "down";

    private final MonitoredServiceRepository repository;
    private final SystemServiceManager systemServiceManager;
    private final TelegramBot telegramBot;

    public ServiceMonitor(MonitoredServiceRepository repository,
                        SystemServiceManager systemServiceManager,
                        TelegramBot telegramBot) {
        this.repository = repository;
        this.systemServiceManager = systemServiceManager;
        this.telegramBot = telegramBot;
    }

    @Scheduled(fixedDelay = 30000) // Check every 30 seconds, as per requirements.
    public void checkServiceStatuses() {
        logger.debug("Running scheduled service status check...");
        List<MonitoredService> services = repository.findAll();

        for (var service : services) {
            var currentStatus = systemServiceManager.getServiceStatus(service.getServiceName());
            var lastKnownStatus = service.getLastStatus();

            // If we are checking for the first time, just record the status.
            if (lastKnownStatus == null) {
                service.setLastStatus(currentStatus);
                repository.save(service);
                logger.info("Initial status for service '{}' is '{}'.", service.getServiceName(), currentStatus);
                continue;
            }

            boolean statusChanged = !currentStatus.equalsIgnoreCase(lastKnownStatus);

            if (statusChanged) {
                logger.info("Status change for service '{}' in chat {}: {} -> {}",
                        service.getServiceName(), service.getChatId(), lastKnownStatus, currentStatus);

                // Sending alert to the specific chat that is monitoring the service.
                String alertText = switch (currentStatus) {
                    case STATUS_DOWN -> String.format("⚠ Service DOWN: `%s`", service.getServiceName());
                    case STATUS_RUNNING -> String.format("✅ Service UP: `%s`", service.getServiceName());
                    default -> null;
                };

                if (alertText != null) {
                    telegramBot.sendMessage(service.getChatId(), alertText);
                }

                service.setLastStatus(currentStatus);
                repository.save(service);
            }
        }
    }
}
