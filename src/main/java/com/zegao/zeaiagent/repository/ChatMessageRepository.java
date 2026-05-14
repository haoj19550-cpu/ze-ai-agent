package com.zegao.zeaiagent.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zegao.zeaiagent.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天消息数据访问接口 - MyBatis-Plus Mapper
 */
@Mapper
public interface ChatMessageRepository extends BaseMapper<ChatMessage> {

    /**
     * 根据会话ID查询所有消息，按顺序排序
     */
    @Select("SELECT * FROM chat_messages WHERE conversation_id = #{conversationId} ORDER BY message_order ASC")
    List<ChatMessage> selectByConversationIdOrderByMessageOrderAsc(@Param("conversationId") String conversationId);

    /**
     * 根据会话ID删除所有消息
     */
    @Select("DELETE FROM chat_messages WHERE conversation_id = #{conversationId}")
    void deleteByConversationId(@Param("conversationId") String conversationId);

    /**
     * 查询某个会话的最大消息序号
     */
    @Select("SELECT MAX(message_order) FROM chat_messages WHERE conversation_id = #{conversationId}")
    Integer selectMaxMessageOrderByConversationId(@Param("conversationId") String conversationId);
}
