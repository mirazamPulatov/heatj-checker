package com.example.systemmonitorbot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoredServiceRepository extends JpaRepository<MonitoredService, Long> {

    List<MonitoredService> findByChatId(Long chatId);

    Optional<MonitoredService> findByChatIdAndServiceName(Long chatId, String serviceName);

    @Transactional
    void deleteByChatIdAndServiceName(Long chatId, String serviceName);
}
