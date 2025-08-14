-- V1__init_local_monitor.sql
-- Initial schema for the Local System Monitor Bot

CREATE TABLE monitored_services (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    last_status VARCHAR(50) DEFAULT 'unknown' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Ensures a service can only be monitored once per chat
    CONSTRAINT unique_service_per_chat UNIQUE (chat_id, service_name)
);

-- Index for faster lookups by chat_id, which will be common
CREATE INDEX idx_monitored_services_chat_id ON monitored_services(chat_id);
