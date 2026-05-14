package com.zegao.zeaiagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类 - 用于MySQL持久化存储
 */
@TableName("chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID，用于标识同一轮对话
     */
    @TableField("conversation_id")
    private String conversationId;

    /**
     * 消息内容（JSON格式存储）
     */
    @TableField("message_content")
    private String messageContent;

    /**
     * 消息类型：USER、ASSISTANT、SYSTEM
     */
    @TableField("message_type")
    private String messageType;

    /**
     * 消息顺序，用于保持对话顺序
     */
    @TableField("message_order")
    private Integer messageOrder;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
