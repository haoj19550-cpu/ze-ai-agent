package com.zegao.zeaiagent.agent;

import com.zegao.zeaiagent.Advisor.MyLoggerAdvisor;
import com.zegao.zeaiagent.agent.ToolCallAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class ZeManus extends ToolCallAgent {

    public ZeManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("zeManus");
        String SYSTEM_PROMPT = """
                You are ZeManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                Reply to users in Chinese.
                Do not expose raw tool JSON, internal step numbers, tool arguments, stack traces, or debugging fields.
                Present only useful progress and final results: key findings, decisions, file paths, links, and next actions.
                When tool results are noisy, summarize them into concise human-readable information.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, break the problem down and solve it step by step.
                Keep user-visible output concise and useful. Avoid raw JSON or internal logs.
                If the task is complete, call the terminate tool/function.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
