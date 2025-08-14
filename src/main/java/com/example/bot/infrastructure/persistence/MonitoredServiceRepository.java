package com.example.bot.infrastructure.persistence;

import com.example.bot.domain.MonitoredService;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoredServiceRepository extends JpaRepository<MonitoredService, Long> {

    /**
     * Finds all services monitored by a specific chat.
     * @param chatId The ID of the chat.
     * @return A list of monitored services for that chat.
     */
    List<MonitoredService> findByChatId(long chatId);

    /**
     * Finds a specific service monitored by a specific chat.
     * @param chatId The ID of the chat.
     * @param serviceName The name of the service.
     * @return An Optional containing the service if found.
     */
    Optional<MonitoredService> findByChatIdAndServiceName(long chatId, String serviceName);

    /**
     * Deletes a specific service monitored by a specific chat.
     * This is used for the /unmonitor command.
     * @param chatId The ID of the chat.
     * @param serviceName The name of the service.
     */
    @Transactional
    void deleteByChatIdAndServiceName(long chatId, String serviceName);
}
