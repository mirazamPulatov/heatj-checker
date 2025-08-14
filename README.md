# Local System Monitor Bot

This project is a Java 21 Spring Boot application that acts as a Telegram bot to monitor **local** systemd services. It is designed with multi-chat support, allowing different chats or users to independently monitor their own list of services.

## Features

- **Local Monitoring:** Uses `ProcessBuilder` to execute `systemctl` commands on the same machine where the bot is running.
- **Multi-Chat Support:** Each Telegram chat has its own independent list of services to monitor. Alerts for a service are sent only to the chat that registered it.
- **Full Service Management:** Add, remove, list, check status, start, and stop services directly from Telegram.
- **Database Persistence:** Uses a PostgreSQL database (with Flyway for migrations) to store which services are monitored by which chat, ensuring state survives restarts.
- **Clean Architecture:** Follows a clean, layered architecture for maintainability.
- **Dockerized Database:** Includes a `docker-compose.yml` for easy PostgreSQL setup.

## 1. Getting Started

### Prerequisites

- Java 21
- Maven 3.8+
- Docker and Docker Compose
- The bot must be run on a Linux machine with `systemd`.

### Step 1: Run the Database

A PostgreSQL database is required. You can easily start one using the provided Docker Compose file:

```bash
docker-compose up -d
```

This will start a PostgreSQL container on port `5432`.

### Step 2: Configure the Application

Edit the `src/main/resources/application.properties` file to set up your Telegram Bot token and username.

```properties
telegram.bot.token=YOUR_BOT_TOKEN
telegram.bot.username=YOUR_BOT_USERNAME
```

The database settings are pre-configured to work with the Docker Compose setup.

## 2. Local Server Setup (Crucial)

For the bot to manage services, the **user that runs the Java application** must have passwordless `sudo` access for `systemctl` commands.

1.  Run `sudo visudo` to safely edit the sudoers file.
2.  Add the following line at the end, replacing `your_run_user` with the username that will execute the bot's JAR file:

    ```
    your_run_user ALL=(ALL) NOPASSWD: /bin/systemctl status, /bin/systemctl start, /bin/systemctl stop
    ```

This is required for the bot to manage services without needing a password.

## 3. Building and Running the Application

### Build the JAR
```bash
mvn clean package
```

### Run the Application
```bash
java -jar target/local-monitor-bot-1.0.0.jar
```

The bot will start, connect to the database, apply migrations via Flyway, and begin monitoring any services that are added via the `/monitor` command.

## 4. Bot Commands

- `/monitor <serviceName>`: Starts monitoring the specified service for the current chat.
- `/unmonitor <serviceName>`: Stops monitoring the specified service for the current chat.
- `/list`: Lists all services currently being monitored in this chat.
- `/status <serviceName>`: Checks the current status of any service on the local machine.
- `/start <serviceName>`: Attempts to start the specified service.
- `/stop <serviceName>`: Attempts to stop the specified service.
