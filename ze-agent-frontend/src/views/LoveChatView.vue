<script setup>
import { computed } from 'vue'

import ChatRoom from '@/components/ChatRoom.vue'
import { createChatId } from '@/utils/session'

const chatId = createChatId('love')

const requestConfig = computed(() => ({
  path: '/ai/Love_app/Chat/sse',
  getParams: (message) => ({
    message,
    chatId,
  }),
}))
</script>

<template>
  <ChatRoom
    title="AI 恋爱大师"
    subtitle="温柔、直接、可追问的恋爱聊天助手"
    assistant-name="恋爱大师"
    placeholder="描述你的问题，比如：最近和 TA 聊天总是冷场怎么办？"
    :request-config="requestConfig"
    :chat-id="chatId"
  />
</template>
