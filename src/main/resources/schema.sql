-- 创建数据库
CREATE DATABASE IF NOT EXISTS ze_ai_agent 
    DEFAULT CHARACTER SET utf8mb4 
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ze_ai_agent;

-- 创建聊天消息表
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    conversation_id VARCHAR(100) NOT NULL COMMENT '会话ID',
    message_content TEXT COMMENT '消息内容(JSON格式)',
    message_type VARCHAR(50) NOT NULL COMMENT '消息类型: USER, ASSISTANT, SYSTEM',
    message_order INT COMMENT '消息顺序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_message_order (message_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息存储表';
