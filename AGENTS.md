# System Monitor Bot - Instructions

This document provides instructions on how to build, configure, and run the System Monitor Bot on a Linux VPS.

## 1. Prerequisites

- **Java 11 or higher:** You can check your Java version with `java -version`.
- **Maven:** You can check your Maven version with `mvn -version`.
- **A Linux server:** The bot is designed to run on Linux and uses `systemctl`.

## 2. Configuration

### 2.1. Get a Telegram Bot Token

1.  Open Telegram and search for the **BotFather** bot.
2.  Start a chat with BotFather and send the `/newbot` command.
3.  Follow the instructions to choose a name and username for your bot.
4.  BotFather will give you a **token**. Keep this token secure.

### 2.2. Get your Chat/Channel ID

**For a private chat:**

1.  Search for the **@userinfobot** on Telegram.
2.  Start a chat with it, and it will give you your user ID. This is your chat ID.

**For a channel:**

1.  Add your bot to the channel as an administrator.
2.  Send a temporary message to the channel.
3.  Forward that message to **@userinfobot**.
4.  The bot will reply with the channel's chat ID (it will be a negative number).

### 2.3. Update `application.properties`

1.  Open the `src/main/resources/application.properties` file.
2.  Replace the placeholder values with your actual bot token, username, and channel ID.

```properties
bot.token=YOUR_BOT_TOKEN
bot.username=YOUR_BOT_USERNAME
# If you are using a channel, make sure the channel ID is correct.
# For personal chats, this can be your user ID.
bot.channel.id=YOUR_CHANNEL_ID
```

### 2.4. Choose a Database

The application is configured to use the H2 in-memory database by default. If you want to use PostgreSQL for persistence, you need to:

1.  Make sure you have PostgreSQL installed and a database created.
2.  Comment out the H2 configuration lines in `application.properties`.
3.  Uncomment and fill in the PostgreSQL configuration lines.

## 3. Building the Application

1.  Navigate to the root directory of the project in your terminal.
2.  Run the following Maven command to build the project and create an executable JAR file:

```bash
mvn clean package
```

This will create a JAR file in the `target/` directory, for example, `system-monitor-bot-1.0.0.jar`.

## 4. Running the Bot

### Option 1: Using `nohup` (Simple)

You can run the bot as a background process using `nohup`, which will keep it running even after you close your terminal.

```bash
nohup java -jar target/system-monitor-bot-1.0.0.jar > bot.log 2>&1 &
```

- `nohup` ensures the process doesn't stop when you log out.
- `>` redirects standard output to a file named `bot.log`.
- `2>&1` redirects standard error to the same file.
- `&` runs the process in the background.

To stop the bot, you can find its process ID (`pid`) and kill it:

```bash
ps aux | grep system-monitor-bot
kill <pid>
```

### Option 2: Using `systemd` (Recommended for Production)

For a more robust setup, you can run the bot as a `systemd` service. This will allow the bot to start automatically on system boot and restart if it crashes.

1.  Create a service file:

```bash
sudo nano /etc/systemd/system/system-monitor-bot.service
```

2.  Add the following content to the file. Make sure to replace `/path/to/your/project` with the actual path to the project directory.

```ini
[Unit]
Description=System Monitor Telegram Bot
After=network.target

[Service]
User=your_user # Replace with the user you want to run the bot as
Group=your_group # Replace with the group
WorkingDirectory=/path/to/your/project
ExecStart=/usr/bin/java -jar /path/to/your/project/target/system-monitor-bot-1.0.0.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

3.  Reload `systemd` to recognize the new service:

```bash
sudo systemctl daemon-reload
```

4.  Start the bot:

```bash
sudo systemctl start system-monitor-bot
```

5.  Enable the bot to start on boot:

```bash
sudo systemctl enable system-monitor-bot
```

You can check the status of the bot with:

```bash
sudo systemctl status system-monitor-bot
```

And view its logs with:

```bash
journalctl -u system-monitor-bot -f
```
