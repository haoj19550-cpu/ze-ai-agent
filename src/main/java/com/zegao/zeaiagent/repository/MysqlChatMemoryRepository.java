package com.zegao.zeaiagent.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zegao.zeaiagent.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 MySQL 的对话记忆存储库实现
 * 
 * 设计动机：
 * - Spring AI 的 ChatMemoryRepository 接口提供了统一的存储抽象
 * - 使用 JSON 序列化消息，避免复杂的对象关系映射
 * - 支持多会话隔离，每个 conversationId 对应一轮完整对话
 */
@Component
@Slf4j
public class MysqlChatMemoryRepository implements ChatMemoryRepository {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 用于跟踪每个会话的消息计数
    private final Map<String, Integer> conversationCounters = new ConcurrentHashMap<>();

    /**
     * 保存消息到数据库
     * 
     * @param conversationId 会话ID
     * @param messages 消息列表
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        log.info("=== MysqlChatMemoryRepository.saveAll() 被调用 ===");
        log.info("conversationId: {}", conversationId);
        log.info("messages 数量: {}", messages != null ? messages.size() : 0);
        
        if (messages == null || messages.isEmpty()) {
            log.warn("消息列表为空，跳过保存");
            return;
        }

        // 获取当前会话的最大消息序号
        Integer maxOrder = chatMessageRepository.selectMaxMessageOrderByConversationId(conversationId);
        int currentOrder = (maxOrder != null) ? maxOrder : 0;
        log.info("当前最大消息序号: {}, 下一个序号: {}", maxOrder, currentOrder + 1);

        // 批量保存消息
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            currentOrder++;
            
            try {
                // 将消息序列化为 JSON
                String messageJson = objectMapper.writeValueAsString(message);
                
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setConversationId(conversationId);
                chatMessage.setMessageContent(messageJson);
                chatMessage.setMessageType(message.getMessageType().name());
                chatMessage.setMessageOrder(currentOrder);
                
                // 使用 MyBatis-Plus 的 insert 方法
                int rows = chatMessageRepository.insert(chatMessage);
                log.info("保存第 {}/{} 条消息, 类型: {}, 影响行数: {}", i + 1, messages.size(), message.getMessageType(), rows);
            } catch (JsonProcessingException e) {
                log.error("消息序列化失败", e);
                throw new RuntimeException("消息序列化失败", e);
            }
        }
        
        log.info("=== 消息保存完成 ===");
    }

    @Override
    public List<String> findConversationIds() {
        return List.of();
    }

    /**
     * 从数据库读取消息
     * 
     * @param conversationId 会话ID
     * @return 消息列表
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<ChatMessage> entities = chatMessageRepository.selectByConversationIdOrderByMessageOrderAsc(conversationId);
        List<Message> messages = new ArrayList<>();

        for (ChatMessage entity : entities) {
            try {
                // 从 JSON 反序列化消息
                Message message = deserializeMessage(entity.getMessageContent(), entity.getMessageType());
                if (message != null) {
                    messages.add(message);
                }
            } catch (Exception e) {
                System.err.println("消息反序列化失败，跳过该消息: " + entity.getId());
                e.printStackTrace();
            }
        }

        return messages;
    }


    /**
     * 清除指定会话的所有消息
     * 
     * @param conversationId 会话ID
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        chatMessageRepository.deleteByConversationId(conversationId);
        conversationCounters.remove(conversationId);
    }

    /**
     * 根据消息类型反序列化消息
     */
    private Message deserializeMessage(String json, String messageType) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(json);
        String text = root.path("text").asText("");

        return switch (messageType) {
            case "USER" -> new UserMessage(text);
            case "ASSISTANT" -> new AssistantMessage(text);
            case "SYSTEM" -> new SystemMessage(text);
            default -> {
                System.err.println("未知的消息类型: " + messageType);
                yield null;
            }
        };
    }
}
