package com.zegao.zeaiagent.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zegao.zeaiagent.entity.ChatMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageRepository extends BaseMapper<ChatMessage> {

    @Select("SELECT DISTINCT conversation_id FROM chat_messages ORDER BY conversation_id ASC")
    List<String> selectDistinctConversationIds();

    @Select("SELECT * FROM chat_messages WHERE conversation_id = #{conversationId} ORDER BY message_order ASC, id ASC")
    List<ChatMessage> selectByConversationIdOrderByMessageOrderAsc(@Param("conversationId") String conversationId);

    @Delete("DELETE FROM chat_messages WHERE conversation_id = #{conversationId}")
    int deleteByConversationId(@Param("conversationId") String conversationId);

}
