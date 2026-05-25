package com.zegao.zeaiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zegao.zeaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    private final ToolCallback[] availableTools;

    private ChatResponse toolCallChatResponse;

    private final ToolCallingManager toolCallingManager;

    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(availableTools)
                .internalToolExecutionEnabled(false)
                .build();
    }

    @Override
    public boolean think() {
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }

        emitStream("正在分析你的需求...\n");
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, chatOptions);

        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .call()
                    .chatResponse();

            this.toolCallChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            String result = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();

            log.info("{} thinking: {}", getName(), result);
            log.info("{} selected {} tool(s)", getName(), toolCallList.size());
            log.info(toolCallList.stream()
                    .map(toolCall -> String.format("tool=%s, arguments=%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n")));

            if (toolCallList.isEmpty()) {
                getMessageList().add(assistantMessage);
                if (hasText(result)) {
                    emitStream(result.trim() + "\n");
                }
                setState(AgentState.FINISHED);
                return false;
            }

            emitStream(buildToolPlanMessage(toolCallList));
            return true;
        } catch (Exception e) {
            log.error("{} thinking failed: {}", getName(), e.getMessage(), e);
            String error = "处理时遇到问题：" + e.getMessage();
            getMessageList().add(new AssistantMessage(error));
            emitStream(error + "\n");
            return false;
        }
    }

    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "";
        }

        emitStream("正在获取信息并整理结果...\n");
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        setMessageList(toolExecutionResult.conversationHistory());

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        String userVisibleResult = summarizeToolResponses(toolResponseMessage);

        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
        }

        if (hasText(userVisibleResult)) {
            emitStream(userVisibleResult + "\n");
        }
        log.info("tool result summary: {}", userVisibleResult);
        return userVisibleResult;
    }

    private String buildToolPlanMessage(List<AssistantMessage.ToolCall> toolCallList) {
        String tools = toolCallList.stream()
                .map(toolCall -> displayToolName(toolCall.name()))
                .distinct()
                .collect(Collectors.joining("、"));
        return "我会先使用" + tools + "，拿到关键信息后再继续整理。\n";
    }

    private String summarizeToolResponses(ToolResponseMessage toolResponseMessage) {
        return toolResponseMessage.getResponses().stream()
                .map(response -> summarizeToolResponse(response.name(), String.valueOf(response.responseData())))
                .filter(this::hasText)
                .collect(Collectors.joining("\n\n"));
    }

    private String summarizeToolResponse(String toolName, String rawData) {
        if (!hasText(rawData)) {
            return "";
        }

        String lowerToolName = toolName == null ? "" : toolName.toLowerCase(Locale.ROOT);
        if (lowerToolName.contains("search")) {
            return summarizeSearchResults(rawData);
        }

        if (lowerToolName.contains("terminate")) {
            return "任务已完成。";
        }

        String cleaned = rawData.trim();
        if (looksLikeStructuredData(cleaned) && cleaned.length() > 300) {
            return displayToolName(toolName) + "已完成，正在基于结果继续整理。";
        }

        return limitText(cleaned, 600);
    }

    private String summarizeSearchResults(String rawData) {
        List<JSONObject> results = parseSearchResults(rawData);
        if (results.isEmpty()) {
            return "搜索已完成，正在基于搜索结果继续整理。";
        }

        StringBuilder summary = new StringBuilder("我找到了这些可参考的信息：\n");
        int count = Math.min(results.size(), 3);
        for (int i = 0; i < count; i++) {
            JSONObject result = results.get(i);
            String title = firstText(result, "title", "name");
            String snippet = firstText(result, "snippet", "description");
            String link = firstText(result, "displayed_link", "link");

            summary.append(i + 1).append(". ").append(hasText(title) ? title : "相关结果").append("\n");
            if (hasText(snippet)) {
                summary.append("   ").append(limitText(snippet, 90)).append("\n");
            }
            if (hasText(link)) {
                summary.append("   来源：").append(link).append("\n");
            }
        }

        return summary.toString().trim();
    }

    private List<JSONObject> parseSearchResults(String rawData) {
        String trimmed = rawData.trim();
        try {
            JSONArray array = trimmed.startsWith("[")
                    ? JSONUtil.parseArray(trimmed)
                    : JSONUtil.parseArray("[" + trimmed + "]");
            return array.stream()
                    .filter(JSONObject.class::isInstance)
                    .map(JSONObject.class::cast)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to parse search result for display summary", e);
            return List.of();
        }
    }

    private String firstText(JSONObject object, String... keys) {
        for (String key : keys) {
            String value = object.getStr(key);
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String displayToolName(String toolName) {
        if (!hasText(toolName)) {
            return "工具";
        }

        String lowerToolName = toolName.toLowerCase(Locale.ROOT);
        if (lowerToolName.contains("search")) {
            return "网络搜索";
        }
        if (lowerToolName.contains("pdf")) {
            return "PDF 生成";
        }
        if (lowerToolName.contains("download")) {
            return "资源下载";
        }
        if (lowerToolName.contains("file")) {
            return "文件处理";
        }
        if (lowerToolName.contains("terminal")) {
            return "终端执行";
        }
        if (lowerToolName.contains("terminate")) {
            return "完成任务";
        }
        return toolName;
    }

    private boolean looksLikeStructuredData(String value) {
        return value.startsWith("{") || value.startsWith("[") || value.contains("\":");
    }

    private String limitText(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
