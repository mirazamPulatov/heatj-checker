package com.example.systemmonitorbot;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Data
@NoArgsConstructor
@Entity
@Table(name = "monitored_services", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chatId", "serviceName"})
})
public class MonitoredService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long chatId;

    private String serviceName;

    private String lastKnownStatus;

    public MonitoredService(Long chatId, String serviceName) {
        this.chatId = chatId;
        this.serviceName = serviceName;
    }
}
