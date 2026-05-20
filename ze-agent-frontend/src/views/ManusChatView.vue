<script setup>
import { computed } from 'vue'

import ChatRoom from '@/components/ChatRoom.vue'

const requestConfig = computed(() => ({
  path: '/ai/manus/chat',
  getParams: (message) => ({
    message,
  }),
}))
</script>

<template>
  <ChatRoom
    title="AI 超级智能体"
    subtitle="面向复杂任务的实时智能体对话"
    assistant-name="超级智能体"
    placeholder="输入你想交给智能体处理的任务..."
    :request-config="requestConfig"
  />
</template>
