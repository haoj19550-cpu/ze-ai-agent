<script setup>
import ChatRoom from '@/components/ChatRoom.vue'

const requestConfig = {
  path: '/ai/Love_app/Chat/sse',
  getParams: (message, chatId) => ({
    message,
    chatId,
  }),
}
</script>

<template>
  <ChatRoom
    title="AI 恋爱大师"
    subtitle="温柔、直接、可追问的恋爱聊天助手"
    assistant-name="恋爱大师"
    placeholder="描述你的问题，比如：最近和 TA 聊天总是冷场怎么办？"
    app-type="love"
    chat-id-prefix="love"
    :request-config="requestConfig"
  />
</template>
