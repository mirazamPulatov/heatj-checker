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

        for (MonitoredService service : services) {
            String currentStatus = systemServiceManager.getServiceStatus(service.getServiceName());
            String lastKnownStatus = service.getLastStatus();

            // If we are checking for the first time, just record the status.
            if (lastKnownStatus == null) {
                service.setLastStatus(currentStatus);
                repository.save(service);
                continue;
            }

            boolean statusChanged = !currentStatus.equalsIgnoreCase(lastKnownStatus);

            if (statusChanged) {
                logger.info("Status change for service '{}': {} -> {}", service.getServiceName(), lastKnownStatus, currentStatus);

                // Sending alert to the specific chat that is monitoring the service.
                if (STATUS_DOWN.equalsIgnoreCase(currentStatus)) {
                    telegramBot.sendMessage(service.getChatId(), "⚠ Service DOWN: `" + service.getServiceName() + "`");
                } else if (STATUS_RUNNING.equalsIgnoreCase(currentStatus)) {
                    telegramBot.sendMessage(service.getChatId(), "✅ Service UP: `" + service.getServiceName() + "`");
                }

                service.setLastStatus(currentStatus);
                repository.save(service);
            }
        }
    }
}
