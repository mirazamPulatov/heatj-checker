# Production System Monitor Telegram Bot

This project is a production-ready Java Spring Boot application that acts as a Telegram bot to monitor Linux system services using `systemctl`. It sends alerts to a configured Telegram channel when a service's status changes.

## Features

- **Centralized Alerting:** All alerts are sent to a single, configured Telegram channel.
- **Service Monitoring:** Add, remove, and list services to monitor per chat.
- **Detailed Status Checks:** Get the full output of `systemctl status` for any service.
- **Persistent Storage:** Uses PostgreSQL to store the list of monitored services.
- **High-Frequency Checks:** Monitors services every 30 seconds.
- **Clean Architecture:** Follows a standard Controller-Service-Repository pattern.

## 1. Prerequisites

- **Java 11** or higher
- **Maven 3.6** or higher
- **PostgreSQL Server**
- A **Linux** server with `systemd`

## 2. Database Setup

You need a PostgreSQL database and a user for the bot.

1.  Connect to your PostgreSQL server.
2.  Create a database and a user. Replace `your_password` with a strong password.

```sql
CREATE DATABASE system_monitor_db;
CREATE USER system_monitor_user WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE system_monitor_db TO system_monitor_user;
```

## 3. Bot Configuration

### 3.1. Create a Telegram Bot

1.  Talk to **@BotFather** on Telegram.
2.  Use the `/newbot` command to create a bot.
3.  Note the **HTTP API token** he gives you.
4.  Set a username for your bot (e.g., `MySystemMonitorBot`).

### 3.2. Create a Telegram Channel

1.  Create a new **public or private channel** where the bot will post alerts.
2.  Add your newly created bot to the channel as an **administrator**.
3.  Get the **Channel ID**.
    - For public channels, the ID is the username (e.g., `@my_channel_name`).
    - For private channels, send a message to the channel and forward it to a bot like **@userinfobot** to get the numerical channel ID (it will start with `-100...`).

### 3.3. Configure `application.properties`

Open `src/main/resources/application.properties` and fill in the details:

```properties
# Telegram Bot Configuration
telegram.bot.token=YOUR_HTTP_API_TOKEN
telegram.bot.username=YOUR_BOT_USERNAME
telegram.channel.id=YOUR_CHANNEL_ID # e.g., @my_public_channel or -100123456789

# PostgreSQL Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/system_monitor_db
spring.datasource.username=system_monitor_user
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

## 4. Security: Passwordless Sudo

The application uses `sudo systemctl status` to get detailed service information. You must grant the user that will run the application passwordless `sudo` access **only for this specific command**.

1.  Run `sudo visudo` to safely edit the sudoers file.
2.  Add the following line at the end of the file. Replace `your_run_user` with the actual username that will run the bot JAR file.

```
your_run_user ALL=(ALL) NOPASSWD: /bin/systemctl status
```

**Warning:** This is a critical security step. Ensure the path to `systemctl` is correct (`which systemctl` can help you find it) and that you only grant `NOPASSWD` access to this one command.

## 5. Building the Application

Navigate to the project's root directory and build the executable JAR file using Maven:

```bash
mvn clean package
```

This will generate `system-monitor-bot-prod-1.0.0.jar` in the `target/` directory.

## 6. Running as a Systemd Service (Production)

Running the bot as a `systemd` service ensures it starts on boot and restarts if it fails.

1.  Create a service file:

    ```bash
    sudo nano /etc/systemd/system/system-monitor-bot.service
    ```

2.  Add the following configuration. **Update the `User`, `Group`, and `WorkingDirectory`/`ExecStart` paths** to match your setup.

    ```ini
    [Unit]
    Description=System Monitor Telegram Bot
    After=network.target postgresql.service

    [Service]
    User=your_run_user
    Group=your_run_user
    WorkingDirectory=/path/to/your/project/
    ExecStart=/usr/bin/java -jar /path/to/your/project/target/system-monitor-bot-prod-1.0.0.jar
    SuccessExitStatus=143
    Restart=on-failure
    RestartSec=10

    [Install]
    WantedBy=multi-user.target
    ```

3.  Reload the systemd daemon, start the service, and enable it on boot:

    ```bash
    sudo systemctl daemon-reload
    sudo systemctl start system-monitor-bot
    sudo systemctl enable system-monitor-bot
    ```

4.  You can check the service's status and logs with:

    ```bash
    sudo systemctl status system-monitor-bot
    sudo journalctl -u system-monitor-bot -f
    ```
