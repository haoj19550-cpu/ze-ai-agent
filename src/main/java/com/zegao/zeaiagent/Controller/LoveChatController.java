package com.zegao.zeaiagent.Controller;

import com.zegao.zeaiagent.agent.ZeManus;
import com.zegao.zeaiagent.demo.invok.LoveApp;
import com.zegao.zeaiagent.entity.ChatMessage;
import com.zegao.zeaiagent.repository.ChatMessageRepository;
import com.zegao.zeaiagent.repository.MysqlChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
public class LoveChatController {

    @Autowired
    private LoveApp loveApp;

    @Autowired
    private ToolCallback[] allTools;

    @Autowired
    private ChatModel dashscopeChatModel;

    @Autowired
    private MysqlChatMemoryRepository mysqlChatMemoryRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    /**
     * 恋爱聊天对话 (SSE流式输出)
     */
    @GetMapping(value = "/Love_app/Chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithSse(@RequestParam String message, @RequestParam String chatId) {
        return loveApp.doChatByStream(message, chatId);
    }

    /**
     * Ai智能体（流式输出）
     */
    @GetMapping(value = "/manus/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithManus(@RequestParam String message,
                                      @RequestParam(required = false) String chatId) {
        String conversationId = StringUtils.hasText(chatId) ? chatId : "manus-" + UUID.randomUUID();
        List<Message> visibleHistory = new ArrayList<>(mysqlChatMemoryRepository.findByConversationId(conversationId));

        ZeManus zeManus = new ZeManus(allTools, dashscopeChatModel);
        zeManus.setMessageList(new ArrayList<>(visibleHistory));

        return zeManus.runStream(message, assistantContent ->
                saveManusConversation(conversationId, visibleHistory, message, assistantContent));
    }

    @GetMapping("/chats")
    public List<ChatSessionResponse> listChats(@RequestParam String appType) {
        String prefix = resolveChatPrefix(appType);
        return chatMessageRepository.selectConversationIdsByPrefix(prefix)
                .stream()
                .map(this::toChatSession)
                .toList();
    }

    @GetMapping("/chats/{chatId}/messages")
    public List<ChatMessageResponse> listMessages(@PathVariable String chatId) {
        List<Message> messages = mysqlChatMemoryRepository.findByConversationId(chatId);
        List<ChatMessageResponse> responses = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            String role = toVisibleRole(message);
            if (role != null && StringUtils.hasText(message.getText())) {
                responses.add(new ChatMessageResponse(role, message.getText(), i + 1));
            }
        }

        return responses;
    }

    private void saveManusConversation(String chatId,
                                       List<Message> history,
                                       String userMessage,
                                       String assistantContent) {
        if (!StringUtils.hasText(userMessage)) {
            return;
        }

        List<Message> messages = new ArrayList<>(history);
        messages.add(new UserMessage(userMessage));

        if (StringUtils.hasText(assistantContent)) {
            messages.add(new AssistantMessage(assistantContent));
        }

        mysqlChatMemoryRepository.saveAll(chatId, messages);
    }

    private ChatSessionResponse toChatSession(String chatId) {
        List<ChatMessage> entities = chatMessageRepository.selectByConversationIdOrderByMessageOrderAsc(chatId);
        List<Message> messages = mysqlChatMemoryRepository.findByConversationId(chatId);
        return new ChatSessionResponse(chatId, buildTitle(messages), findUpdatedAt(entities));
    }

    private String buildTitle(List<Message> messages) {
        return messages.stream()
                .filter(message -> "USER".equals(message.getMessageType().name()))
                .map(Message::getText)
                .filter(StringUtils::hasText)
                .findFirst()
                .map(this::truncateTitle)
                .orElse("\u65b0\u804a\u5929");
    }

    private String truncateTitle(String title) {
        String normalized = title.trim().replaceAll("\\s+", " ");
        int maxCodePoints = 20;
        if (normalized.codePointCount(0, normalized.length()) <= maxCodePoints) {
            return normalized;
        }

        int endIndex = normalized.offsetByCodePoints(0, maxCodePoints);
        return normalized.substring(0, endIndex) + "...";
    }

    private LocalDateTime findUpdatedAt(List<ChatMessage> messages) {
        return messages.stream()
                .map(ChatMessage::getCreatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private String toVisibleRole(Message message) {
        return switch (message.getMessageType().name()) {
            case "USER" -> "user";
            case "ASSISTANT" -> "assistant";
            default -> null;
        };
    }

    private String resolveChatPrefix(String appType) {
        if (!StringUtils.hasText(appType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appType is required");
        }

        return switch (appType.toLowerCase(Locale.ROOT)) {
            case "love" -> "love-";
            case "manus" -> "manus-";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported appType: " + appType);
        };
    }

    public record ChatSessionResponse(String chatId, String title, LocalDateTime updatedAt) {
    }

    public record ChatMessageResponse(String role, String content, Integer order) {
    }
}
