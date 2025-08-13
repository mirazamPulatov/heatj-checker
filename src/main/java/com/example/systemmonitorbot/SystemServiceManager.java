package com.example.systemmonitorbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class SystemServiceManager {

    private static final Logger logger = LoggerFactory.getLogger(SystemServiceManager.class);

    public String getServiceStatus(String serviceName) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("systemctl", "is-active", serviceName);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            StringBuilder result = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            }

            int exitCode = process.waitFor();
            String output = result.toString().trim();

            if (exitCode == 0) {
                return output;
            } else {
                // is-active returns a non-zero exit code for inactive/failed statuses
                if (output.isEmpty()) {
                    return "unknown";
                }
                return output;
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Error checking status of service {}: {}", serviceName, e.getMessage());
            Thread.currentThread().interrupt();
            return "error";
        }
    }
}
