package com.example.systemmonitorbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Service
public class SystemServiceManager {

    private static final Logger logger = LoggerFactory.getLogger(SystemServiceManager.class);
    private static final String RUNNING_STATUS_IDENTIFIER = "Active: active (running)";

    /**
     * Executes a system command and returns its full output.
     */
    private String executeCommand(String... command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Wait for the process to complete, with a timeout
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                logger.warn("Command timed out: {}", String.join(" ", command));
                process.destroy();
                return "Error: Command timed out.";
            }

            // systemctl status returns exit code 3 for inactive/failed services
            // We don't check the exit code here because we want the text output regardless.

            return output.toString();

        } catch (IOException | InterruptedException e) {
            logger.error("Error executing command '{}': {}", String.join(" ", command), e.getMessage());
            Thread.currentThread().interrupt();
            return "Error: Could not execute command.";
        }
    }

    /**
     * Gets the full, raw output of the 'systemctl status' command.
     *
     * @param serviceName The name of the service.
     * @return The raw status output.
     */
    public String getRawServiceStatus(String serviceName) {
        // Using "sudo" as per security requirements.
        // The user running the bot must have passwordless sudo access for this command.
        return executeCommand("sudo", "systemctl", "status", serviceName);
    }

    /**
     * Determines if a service is running based on its status output.
     *
     * @param serviceName The name of the service.
     * @return "running" if the service is active, "down" otherwise.
     */
    public String getServiceStatus(String serviceName) {
        String rawStatus = getRawServiceStatus(serviceName);
        if (rawStatus.contains(RUNNING_STATUS_IDENTIFIER)) {
            return "running";
        }
        return "down";
    }

    /**
     * Starts a service using 'systemctl start'.
     *
     * @param serviceName The name of the service.
     * @return The command output.
     */
    public String startService(String serviceName) {
        return executeCommand("sudo", "systemctl", "start", serviceName);
    }

    /**
     * Stops a service using 'systemctl stop'.
     *
     * @param serviceName The name of the service.
     * @return The command output.
     */
    public String stopService(String serviceName) {
        return executeCommand("sudo", "systemctl", "stop", serviceName);
    }
}
