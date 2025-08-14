package com.example.bot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Data
@NoArgsConstructor
@Entity
@Table(name = "monitored_services", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"chat_id", "service_name"})
})
public class MonitoredService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "last_status", nullable = false)
    private String lastStatus = "unknown";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MonitoredService(Long chatId, String serviceName) {
        this.chatId = chatId;
        this.serviceName = serviceName;
    }
}
