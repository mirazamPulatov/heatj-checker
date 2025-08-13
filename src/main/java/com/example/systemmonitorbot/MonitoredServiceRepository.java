package com.example.systemmonitorbot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoredServiceRepository extends JpaRepository<MonitoredService, Long> {

    List<MonitoredService> findByChatId(Long chatId);

    Optional<MonitoredService> findByChatIdAndServiceName(Long chatId, String serviceName);
}
