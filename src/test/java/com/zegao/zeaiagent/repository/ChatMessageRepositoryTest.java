package com.zegao.zeaiagent.repository;

import com.zegao.zeaiagent.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 MyBatis-Plus Mapper 是否正常工作
 */
@SpringBootTest
@Slf4j
public class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Test
    void testInsert() {
        // 创建测试消息
        ChatMessage message = new ChatMessage();
        message.setConversationId("test-001");
        message.setMessageContent("{\"text\":\"测试消息\"}");
        message.setMessageType("USER");
        message.setMessageOrder(1);
        message.setCreatedAt(LocalDateTime.now());

        // 插入数据库
        int rows = chatMessageRepository.insert(message);
        log.info("插入影响行数: {}", rows);
        
        // 验证
        assertEquals(1, rows);
        assertNotNull(message.getId());
        log.info("插入成功，生成的ID: {}", message.getId());
    }

    @Test
    void testSelect() {
        // 先插入一条数据
        ChatMessage message = new ChatMessage();
        message.setConversationId("test-002");
        message.setMessageContent("{\"text\":\"查询测试\"}");
        message.setMessageType("USER");
        message.setMessageOrder(1);
        message.setCreatedAt(LocalDateTime.now());
        
        chatMessageRepository.insert(message);
        log.info("插入测试数据，ID: {}", message.getId());

        // 查询
        var messages = chatMessageRepository.selectByConversationIdOrderByMessageOrderAsc("test-002");
        log.info("查询到 {} 条消息", messages.size());
        
        assertFalse(messages.isEmpty());
        assertEquals("test-002", messages.get(0).getConversationId());
    }

    @Test
    void testDelete() {
        // 先插入一条数据
        ChatMessage message = new ChatMessage();
        message.setConversationId("test-003");
        message.setMessageContent("{\"text\":\"删除测试\"}");
        message.setMessageType("USER");
        message.setMessageOrder(1);
        message.setCreatedAt(LocalDateTime.now());
        
        chatMessageRepository.insert(message);
        log.info("插入测试数据，ID: {}", message.getId());

        // 删除
        chatMessageRepository.deleteByConversationId("test-003");
        
        // 验证删除
        var messages = chatMessageRepository.selectByConversationIdOrderByMessageOrderAsc("test-003");
        assertTrue(messages.isEmpty());
        log.info("删除成功");
    }
}
