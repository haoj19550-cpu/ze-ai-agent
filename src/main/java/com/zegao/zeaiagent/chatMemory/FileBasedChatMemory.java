package com.zegao.zeaiagent.chatMemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * 基于文件持久化的对话记忆
 */
public class FileBasedChatMemory implements ChatMemory {

    private final String BASE_DIR;
    private static final Kryo kryo = new Kryo();

    static {
        // 允许序列化未注册的类（兼容性优先）
        kryo.setRegistrationRequired(false);
        // 设置实例化策略，支持无默认构造函数的类
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        // 启用引用跟踪，处理循环引用
        kryo.setReferences(true);
        // 注册常用类以提高性能
        kryo.register(ArrayList.class);
        kryo.register(Collections.emptyList().getClass());
        kryo.register(UserMessage.class);
        kryo.register(AssistantMessage.class);
        kryo.register(SystemMessage.class);
    }

    // 构造对象时，指定文件保存目录
    public FileBasedChatMemory(String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    @Override
    public void add(String conversationId, Message message) {
        // 使用可变 ArrayList 而非 List.of()
        saveConversation(conversationId, new ArrayList<>(List.of(message)));
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> conversationMessages = getOrCreateConversation(conversationId);
        // 确保是可变的 ArrayList
        if (!(conversationMessages instanceof ArrayList)) {
            conversationMessages = new ArrayList<>(conversationMessages);
        }
        conversationMessages.addAll(messages);
        saveConversation(conversationId, conversationMessages);
    }

    @Override
    public List<Message> get(String conversationId) {
        List<Message> allMessages = getOrCreateConversation(conversationId);
        return allMessages;
    }

//    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> allMessages = getOrCreateConversation(conversationId);
        return allMessages.stream()
                .skip(Math.max(0, allMessages.size() - lastN))
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                Object obj = kryo.readClassAndObject(input);
                if (obj instanceof List) {
                    // 确保返回的是可变 ArrayList
                    messages = new ArrayList<>((List<Message>) obj);
                }
            } catch (Exception e) {
                // 如果反序列化失败，删除损坏的文件并返回空列表
                System.err.println("反序列化失败，删除损坏的文件: " + file.getAbsolutePath());
                e.printStackTrace();
                file.delete();
            }
        }
        return messages;
    }

    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            // 使用 writeClassAndObject 以支持多态类型
            kryo.writeClassAndObject(output, messages);
            output.flush();
        } catch (IOException e) {
            System.err.println("序列化失败: " + file.getAbsolutePath());
            e.printStackTrace();
            // 删除可能损坏的文件
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}
