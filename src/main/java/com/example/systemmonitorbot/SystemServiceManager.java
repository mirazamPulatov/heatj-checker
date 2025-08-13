package com.example.systemmonitorbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class SystemServiceManager {

    private static final Logger logger = LoggerFactory.getLogger(SystemServiceManager.class);
    private static final String RUNNING_STATUS_IDENTIFIER = "Active: active (running)";
    private static final int COMMAND_TIMEOUT_SECONDS = 10;

    // Using constants for commands to avoid magic strings and for easier management.
    private static final String SUDO = "sudo";
    private static final String SYSTEMCTL = "systemctl";
    private static final String STATUS_ACTION = "status";
    private static final String START_ACTION = "start";
    private static final String STOP_ACTION = "stop";

    /**
     * Executes a system command and returns its full output.
     * This method is private and encapsulates the ProcessBuilder logic.
     */
    private String executeCommand(String... command) {
        try {
            var processBuilder = new ProcessBuilder(command);
            var process = processBuilder.start();

            // Reading the output from the process's input stream
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.warn("Command timed out: {}", String.join(" ", command));
                process.destroyForcibly();
                return "Error: Command timed out.";
            }

            // systemctl status returns exit code 3 for inactive/failed services.
            // For start/stop, a non-zero exit code indicates failure.
            // We return the full output regardless, as it often contains useful error messages.
            if (process.exitValue() != 0) {
                 String errorOutput = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                 if (!errorOutput.isBlank()) {
                     return output + "\n" + errorOutput;
                 }
            }

            return output;

        } catch (IOException e) {
            logger.error("Error executing command '{}': {}", String.join(" ", command), e.getMessage(), e);
            return "Error: Could not execute command due to an I/O error.";
        } catch (InterruptedException e) {
            logger.error("Command execution was interrupted: {}", String.join(" ", command), e);
            Thread.currentThread().interrupt();
            return "Error: Command execution was interrupted.";
        }
    }

    /**
     * Gets the full, raw output of the 'systemctl status' command.
     *
     * @param serviceName The name of the service.
     * @return The raw status output.
     */
    public String getRawServiceStatus(String serviceName) {
        return executeCommand(SUDO, SYSTEMCTL, STATUS_ACTION, serviceName);
    }

    /**
     * Determines if a service is running based on its status output.
     *
     * @param serviceName The name of the service.
     * @return "running" if the service is active, "down" otherwise.
     */
    public String getServiceStatus(String serviceName) {
        var rawStatus = getRawServiceStatus(serviceName);
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
        return executeCommand(SUDO, SYSTEMCTL, START_ACTION, serviceName);
    }

    /**
     * Stops a service using 'systemctl stop'.
     *
     * @param serviceName The name of the service.
     * @return The command output.
     */
    public String stopService(String serviceName) {
        return executeCommand(SUDO, SYSTEMCTL, STOP_ACTION, serviceName);
    }
}
