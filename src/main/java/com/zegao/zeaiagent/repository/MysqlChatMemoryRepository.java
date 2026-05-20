package com.zegao.zeaiagent.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zegao.zeaiagent.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class MysqlChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMessageRepository chatMessageRepository;

    private final ObjectMapper objectMapper;

    public MysqlChatMemoryRepository(ChatMessageRepository chatMessageRepository, ObjectMapper objectMapper) {
        this.chatMessageRepository = chatMessageRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<String> findConversationIds() {
        return chatMessageRepository.selectDistinctConversationIds();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");

        List<ChatMessage> entities = chatMessageRepository.selectByConversationIdOrderByMessageOrderAsc(conversationId);
        List<Message> messages = new ArrayList<>(entities.size());

        for (ChatMessage entity : entities) {
            Message message = deserializeMessage(entity);
            if (message != null) {
                messages.add(message);
            }
        }

        return messages;
    }

    @Override
    @Transactional
    public void saveAll(String conversationId, List<Message> messages) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.notNull(messages, "messages cannot be null");
        Assert.noNullElements(messages, "messages cannot contain null elements");

        chatMessageRepository.deleteByConversationId(conversationId);

        for (int i = 0; i < messages.size(); i++) {
            chatMessageRepository.insert(toEntity(conversationId, messages.get(i), i + 1));
        }
    }

    @Override
    @Transactional
    public void deleteByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        chatMessageRepository.deleteByConversationId(conversationId);
    }

    private ChatMessage toEntity(String conversationId, Message message, int messageOrder) {
        try {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setConversationId(conversationId);
            chatMessage.setMessageContent(objectMapper.writeValueAsString(new StoredMessage(message.getText())));
            chatMessage.setMessageType(message.getMessageType().name());
            chatMessage.setMessageOrder(messageOrder);
            return chatMessage;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize chat memory message", e);
        }
    }

    private Message deserializeMessage(ChatMessage entity) {
        if (!StringUtils.hasText(entity.getMessageContent())) {
            log.warn("Skip empty chat memory row: id={}", entity.getId());
            return null;
        }

        try {
            String text = readText(entity.getMessageContent());
            return switch (entity.getMessageType()) {
                case "USER" -> new UserMessage(text);
                case "ASSISTANT" -> new AssistantMessage(text);
                case "SYSTEM" -> new SystemMessage(text);
                default -> {
                    log.warn("Skip unsupported chat memory message type: id={}, type={}",
                            entity.getId(), entity.getMessageType());
                    yield null;
                }
            };
        } catch (JsonProcessingException e) {
            log.warn("Skip unreadable chat memory row: id={}", entity.getId(), e);
            return null;
        }
    }

    private String readText(String messageContent) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(messageContent);
        if (root.isTextual()) {
            return root.asText();
        }

        JsonNode text = root.path("text");
        if (!text.isMissingNode()) {
            return text.asText("");
        }

        JsonNode textContent = root.path("textContent");
        if (!textContent.isMissingNode()) {
            return textContent.asText("");
        }

        JsonNode content = root.path("content");
        if (!content.isMissingNode()) {
            return content.asText("");
        }

        return "";
    }

    private record StoredMessage(String text) {
    }
}
