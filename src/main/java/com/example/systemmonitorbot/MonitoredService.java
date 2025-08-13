package com.example.systemmonitorbot;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

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

    @Column(nullable = false)
    private Long chatId;

    @Column(nullable = false)
    private String serviceName;

    private String lastStatus;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public MonitoredService(Long chatId, String serviceName) {
        this.chatId = chatId;
        this.serviceName = serviceName;
    }
}
