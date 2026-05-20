<script setup>
import ChatRoom from '@/components/ChatRoom.vue'

const requestConfig = {
  path: '/ai/manus/chat',
  getParams: (message, chatId) => ({
    message,
    chatId,
  }),
}
</script>

<template>
  <ChatRoom
    title="AI 超级智能体"
    subtitle="面向复杂任务的实时智能体对话"
    assistant-name="超级智能体"
    placeholder="输入你想交给智能体处理的任务..."
    app-type="manus"
    chat-id-prefix="manus"
    :request-config="requestConfig"
  />
</template>
