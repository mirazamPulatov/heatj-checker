package com.example.systemmonitorbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ServiceMonitor.class);

    private final MonitoredServiceRepository monitoredServiceRepository;
    private final SystemServiceManager systemServiceManager;
    private final TelegramBotService telegramBotService;

    public ServiceMonitor(MonitoredServiceRepository monitoredServiceRepository,
                          SystemServiceManager systemServiceManager,
                          TelegramBotService telegramBotService) {
        this.monitoredServiceRepository = monitoredServiceRepository;
        this.systemServiceManager = systemServiceManager;
        this.telegramBotService = telegramBotService;
    }

    @Scheduled(fixedRate = 600000) // 10 minutes
    public void checkServiceStatus() {
        logger.info("Running scheduled service status check...");
        List<MonitoredService> services = monitoredServiceRepository.findAll();

        for (MonitoredService service : services) {
            String currentStatus = systemServiceManager.getServiceStatus(service.getServiceName());
            String lastKnownStatus = service.getLastKnownStatus();

            if (lastKnownStatus == null) {
                // First time checking, just update the status
                service.setLastKnownStatus(currentStatus);
                monitoredServiceRepository.save(service);
                continue;
            }

            boolean isCurrentlyActive = "active".equalsIgnoreCase(currentStatus);
            boolean wasPreviouslyActive = "active".equalsIgnoreCase(lastKnownStatus);

            if (isCurrentlyActive && !wasPreviouslyActive) {
                logger.info("Service {} is back up.", service.getServiceName());
                telegramBotService.sendMessage(service.getChatId(), "✅ Service UP: " + service.getServiceName());
                service.setLastKnownStatus(currentStatus);
                monitoredServiceRepository.save(service);
            } else if (!isCurrentlyActive && wasPreviouslyActive) {
                logger.warn("Service {} is down.", service.getServiceName());
                telegramBotService.sendMessage(service.getChatId(), "⚠ Service DOWN: " + service.getServiceName());
                service.setLastKnownStatus(currentStatus);
                monitoredServiceRepository.save(service);
            }
        }
    }
}
