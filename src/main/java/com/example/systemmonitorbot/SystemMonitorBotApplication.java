package com.example.systemmonitorbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SystemMonitorBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemMonitorBotApplication.class, args);
    }

}
