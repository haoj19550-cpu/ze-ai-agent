CREATE DATABASE IF NOT EXISTS ze_ai_agent
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ze_ai_agent;

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    conversation_id VARCHAR(100) NOT NULL COMMENT 'Conversation ID',
    message_content TEXT NOT NULL COMMENT 'Serialized message content',
    message_type VARCHAR(50) NOT NULL COMMENT 'USER, ASSISTANT, SYSTEM',
    message_order INT NOT NULL COMMENT 'Message order in conversation',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    UNIQUE KEY uk_conversation_message_order (conversation_id, message_order),
    INDEX idx_conversation_order (conversation_id, message_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chat memory messages';
