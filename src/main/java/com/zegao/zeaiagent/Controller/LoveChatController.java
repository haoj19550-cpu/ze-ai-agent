package com.zegao.zeaiagent.Controller;

import com.zegao.zeaiagent.agent.ZeManus;
import com.zegao.zeaiagent.demo.invok.LoveApp;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class LoveChatController {

    @Autowired
    private LoveApp LoveApp;

    @Autowired
    private ToolCallback[] allTools;

    @Autowired
    private ChatModel dashscopeChatModel;
    /**
     * 恋爱聊天对话 (SSE流式输出)
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping (value = "/Love_app/Chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithSse(String message, String chatId) {
        return LoveApp.doChatByStream(message, chatId);
    }

//    /**
//     *
//     * @param message
//     * @param chatId
//     * @return
//     */
//    @GetMapping ("/Love_app/Chat/sse/emmit")
//    public SseEmitter doChatWithSseEmmit(String message, String chatId) {
//        // 创建一个超时时间较长的 SseEmitter
//        SseEmitter emitter = new SseEmitter(180000L); // 3分钟超时
//
//        LoveApp.doChatByStream(message, chatId)
//                .subscribe(data -> {
//                    try {
//                        emitter.send(data);
//                    } catch (Exception e) {
//                        emitter.completeWithError(e);
//                    }
//                },
//                        // 处理错误
//                        emitter::completeWithError,
//                        // 处理完成
//                        emitter::complete
//
//                );
//        return emitter;
//    }

    /**
     * Ai智能体（流式输出）
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        ZeManus zeManus = new ZeManus(allTools, dashscopeChatModel);
        return zeManus.runStream(message);
    }
}
