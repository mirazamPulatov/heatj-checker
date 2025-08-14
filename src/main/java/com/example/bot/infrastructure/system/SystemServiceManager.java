package com.example.bot.infrastructure.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class SystemServiceManager {

    private static final Logger logger = LoggerFactory.getLogger(SystemServiceManager.class);
    private static final int COMMAND_TIMEOUT_SECONDS = 10;

    private static final String SUDO = "sudo";
    private static final String SYSTEMCTL = "systemctl";
    private static final String STATUS_ACTION = "status";
    private static final String START_ACTION = "start";
    private static final String STOP_ACTION = "stop";

    /**
     * Executes a local system command and returns its full output.
     */
    private String executeLocalCommand(String... command) {
        try {
            var processBuilder = new ProcessBuilder(command);
            var process = processBuilder.start();

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String errorOutput = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.warn("Command timed out: {}", String.join(" ", command));
                process.destroyForcibly();
                return "Error: Command timed out.";
            }

            if (process.exitValue() != 0) {
                logger.warn("Command '{}' failed with exit code {}. Stderr: {}",
                    String.join(" ", command), process.exitValue(), errorOutput);
            }

            // Return both stdout and stderr to give the user full context
            return (output.isBlank() ? "" : output) + (errorOutput.isBlank() ? "" : "\n" + errorOutput);

        } catch (IOException e) {
            logger.error("Error executing command '{}': {}", String.join(" ", command), e.getMessage(), e);
            return "Error: Could not execute command due to an I/O error.";
        } catch (InterruptedException e) {
            logger.error("Command execution was interrupted: {}", String.join(" ", command), e);
            Thread.currentThread().interrupt();
            return "Error: Command execution was interrupted.";
        }
    }

    public String getServiceStatus(String serviceName) {
        return executeLocalCommand(SUDO, SYSTEMCTL, STATUS_ACTION, serviceName);
    }

    public String startService(String serviceName) {
        return executeLocalCommand(SUDO, SYSTEMCTL, START_ACTION, serviceName);
    }

    public String stopService(String serviceName) {
        return executeLocalCommand(SUDO, SYSTEMCTL, STOP_ACTION, serviceName);
    }
}
