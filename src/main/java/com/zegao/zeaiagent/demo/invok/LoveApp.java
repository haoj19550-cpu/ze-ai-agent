package com.zegao.zeaiagent.demo.invok;

import com.zegao.zeaiagent.Advisor.MyLoggerAdvisor;
import com.zegao.zeaiagent.repository.MysqlChatMemoryRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class LoveApp {
      private final ChatClient chatClient;
      private static final String SYSTEM_PROMPT = "你叫小汐，将扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
              "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；恋爱状态询问沟通、习惯差异引发的矛盾；" +
              "已婚状态询问家庭责任与亲属关系处理的问题。引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。\n";
      
      private final MysqlChatMemoryRepository mysqlChatMemoryRepository;

      /**
       * 构造函数注入 - 确保依赖在对象创建时就可用
       * 
       * @param dashScopeChatModel ChatModel
       * @param mysqlChatMemoryRepository MySQL 对话记忆存储库
       */
      public LoveApp(ChatModel dashScopeChatModel, MysqlChatMemoryRepository mysqlChatMemoryRepository){
          log.info("=== LoveApp 构造函数被调用 ===");
          log.info("mysqlChatMemoryRepository 是否为 null: {}", mysqlChatMemoryRepository == null);
          if (mysqlChatMemoryRepository != null) {
              log.info("mysqlChatMemoryRepository 类型: {}", mysqlChatMemoryRepository.getClass().getName());
          }
          
          this.mysqlChatMemoryRepository = mysqlChatMemoryRepository;
          
          // 初始化基于 MySQL 的对话记忆
          ChatMemory chatMemory = MessageWindowChatMemory.builder()
                  .chatMemoryRepository(mysqlChatMemoryRepository)
                  .maxMessages(10)
                  .build();
          
          log.info("ChatMemory 类型: {}", chatMemory.getClass().getName());
          log.info("ChatMemory 创建完成");
          
          chatClient = ChatClient.builder(dashScopeChatModel)
                  .defaultSystem(SYSTEM_PROMPT)
                  .defaultAdvisors(
                          MessageChatMemoryAdvisor.builder(chatMemory).build(),
                          //自定义拦截器
                          new MyLoggerAdvisor()
                  )
                  .build();
          
          log.info("=== LoveApp 初始化完成 ===");
      }

      /**
       * Ai 恋爱大师聊天对话
       * @param message
       * @param chatId
       * @return
       */
      public String doChat(String message,String chatId){
          log.info("=== LoveApp.doChat() 被调用 ===");
          log.info("chatId: {}", chatId);
          log.info("message: {}", message);
          
          return chatClient.prompt()
                  .user(message)
                  .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                  .call()
                  .content();
      }

      /**
       * Ai 恋爱大师聊天对话(流式输出)
       * @param message
       * @param chatId
       * @return
       */
    public Flux<String> doChatByStream(String message, String chatId){
        log.info("=== LoveApp.doChat() 被调用 ===");
        log.info("chatId: {}", chatId);
        log.info("message: {}", message);

        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

     record LoveReport(String title, List<String> suggestions) {
    }

    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果,标题为{用户名}的恋爱报告,内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(
                        ChatMemory.CONVERSATION_ID, chatId)
                )
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    @Resource
    private VectorStore loveAppVectorStore;
    //基于恋爱知识库问答
    public String doChatWithRag(String message,String chatId){
        String content = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(QuestionAnswerAdvisor.builder(loveAppVectorStore).build())
                .call()
                .content();
        log.info("恋爱知识库问答输出内容: {}", content);
        return content;
    }

    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .system("When the user asks for images or picture search, use the available MCP image search tool before answering. If page or perPage are missing, call the tool with page=1 and perPage=5. Do not ask relationship-counseling follow-up questions for image-search requests.")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider.getToolCallbacks())
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


//    public String doChat(String message, String chatId) {
//        ChatResponse response = chatClient
//                .prompt()
//                .user(message)
//                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
//                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
//                .call()
//                .chatResponse();
//        String content = response.getResult().getOutput().getText();
//        log.info("content: {}", content);
//        return content;
//    }

}
