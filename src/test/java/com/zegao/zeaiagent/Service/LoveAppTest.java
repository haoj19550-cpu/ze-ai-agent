package com.zegao.zeaiagent.Service;

import com.zegao.zeaiagent.demo.invok.LoveApp;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.UUID;

@SpringBootTest
@Slf4j
public class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        
        // 第一轮
        String message = "你好，我是程序员鱼皮";
        log.info("第一轮提问: {}", message);
        String answer = loveApp.doChat(message, chatId);
        log.info("第一轮回答: {}", answer);
        Assertions.assertNotNull(answer);
        Assertions.assertFalse(answer.isEmpty(), "AI 回答不应为空");
        
        // 第二轮
        message = "我想让另一半（编程导航）更爱我";
        log.info("第二轮提问: {}", message);
        answer = loveApp.doChat(message, chatId);
        log.info("第二轮回答: {}", answer);
        Assertions.assertNotNull(answer);
        Assertions.assertTrue(answer.contains("编程导航") || answer.contains("另一半"), 
                "AI 应该理解并提及'编程导航'或'另一半'");
        
        // 第三轮 - 测试记忆功能
        message = "我的另一半叫什么来着？刚跟你说过，帮我回忆一下";
        log.info("第三轮提问(测试记忆): {}", message);
        answer = loveApp.doChat(message, chatId);
        log.info("第三轮回答: {}", answer);
        Assertions.assertNotNull(answer);
        // 关键断言:AI 应该能记住之前的对话内容
        Assertions.assertTrue(answer.toLowerCase().contains("编程导航") || 
                answer.contains("鱼皮"),
                "AI 应该能回忆起之前提到的'编程导航'或'鱼皮'");
    }


    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是程序员鱼皮，我想让另一半（编程导航）更爱我，但我不知道该怎么做";
        var loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }

    @Test
    void doChat() {
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是泽告，我该如何提升自己在异性间的吸引力呢";
        var content = loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(content);

    }

    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地？");

        // 测试网页抓取：恋爱案例分析
        testMessage("最近和对象吵架了，看看编程导航网站（https://www.bonobology.com/）的其他情侣是怎么解决矛盾的？");

        // 测试资源下载：图片下载
        testMessage("直接下载一张适合做手机壁纸的星空情侣图片为文件");

        // 测试终端操作：执行代码
        testMessage("执行 Python3 脚本来生成数据分析报告");

        // 测试文件操作：保存用户档案
        testMessage("保存我的恋爱档案为文件");

        // 测试 PDF 生成
        testMessage("生成一份‘七夕约会计划’PDF，包含餐厅预订、活动流程和礼物清单");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = loveApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        String message = "我需要一些苏州园林的图片";
        String answer = loveApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
        Assertions.assertTrue(answer.toLowerCase().contains("pexels"));
    }

    @Test
    void mcpSseToolCallbacksAvailable() {
        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
        log.info("MCP SSE tools: {}", Arrays.stream(callbacks)
                .map(callback -> callback.getToolDefinition().name())
                .toList());

        ToolCallback imageSearchTool = Arrays.stream(callbacks)
                .filter(callback -> callback.getToolDefinition().name().contains("searchImages"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("MCP SSE image search tool was not registered"));

        String result = imageSearchTool.call("""
                {"query":"Suzhou garden","page":1,"perPage":1}
                """);
        log.info("MCP SSE direct tool result: {}", result);
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isBlank());
        Assertions.assertTrue(result.contains("success") && result.contains("true"));
    }
}

