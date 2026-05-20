<script setup>
import { nextTick, onBeforeUnmount, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { createChatEventSource } from '@/api/sse'

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    default: '',
  },
  assistantName: {
    type: String,
    default: 'AI',
  },
  placeholder: {
    type: String,
    default: '请输入消息...',
  },
  requestConfig: {
    type: Object,
    required: true,
  },
  chatId: {
    type: String,
    default: '',
  },
})

const inputText = ref('')
const messages = ref([
  {
    id: 'welcome',
    role: 'assistant',
    content: '你好，我已经准备好了。把你的问题发给我，我们开始吧。',
  },
])
const isStreaming = ref(false)
const errorMessage = ref('')
const messageListRef = ref(null)
let activeStream = null

function scrollToBottom() {
  nextTick(() => {
    const list = messageListRef.value
    if (list) {
      list.scrollTop = list.scrollHeight
    }
  })
}

function closeActiveStream() {
  if (activeStream) {
    activeStream.close()
    activeStream = null
  }
}

function appendAssistantContent(messageId, content) {
  const message = messages.value.find((item) => item.id === messageId)
  if (message) {
    message.content += content
  }
  scrollToBottom()
}

function markStreamDone() {
  isStreaming.value = false
  activeStream = null
  scrollToBottom()
}

function sendMessage() {
  const message = inputText.value.trim()
  if (!message || isStreaming.value) {
    return
  }

  closeActiveStream()
  errorMessage.value = ''
  inputText.value = ''

  messages.value.push({
    id: `user-${Date.now()}`,
    role: 'user',
    content: message,
  })

  const assistantMessageId = `assistant-${Date.now()}`
  messages.value.push({
    id: assistantMessageId,
    role: 'assistant',
    content: '',
  })

  isStreaming.value = true
  scrollToBottom()

  activeStream = createChatEventSource(
    props.requestConfig.path,
    props.requestConfig.getParams(message),
    {
      onMessage: (chunk) => appendAssistantContent(assistantMessageId, chunk),
      onDone: markStreamDone,
      onError: () => {
        const current = messages.value.find((item) => item.id === assistantMessageId)

        if (current?.content) {
          markStreamDone()
          return
        }

        errorMessage.value = '连接中断，请确认后端服务已启动后重试。'
        if (current) {
          current.content = '暂时没有收到回复。'
        }
        markStreamDone()
      },
    },
  )
}

function handleKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

onBeforeUnmount(() => {
  closeActiveStream()
})
</script>

<template>
  <main class="chat-page">
    <section class="chat-shell">
      <header class="chat-header">
        <RouterLink class="back-link" to="/" aria-label="返回主页">←</RouterLink>
        <div class="chat-title">
          <h1>{{ title }}</h1>
          <p>{{ subtitle }}</p>
          <span v-if="chatId" class="chat-id">聊天室 ID：{{ chatId }}</span>
        </div>
      </header>

      <div ref="messageListRef" class="message-list" aria-live="polite">
        <article
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="message.role"
        >
          <div class="avatar">{{ message.role === 'user' ? '我' : 'AI' }}</div>
          <div class="message-bubble">
            <span class="message-name">{{ message.role === 'user' ? '你' : assistantName }}</span>
            <p v-if="message.content">{{ message.content }}</p>
            <p v-else class="typing">正在思考...</p>
          </div>
        </article>
      </div>

      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

      <form class="composer" @submit.prevent="sendMessage">
        <textarea
          v-model="inputText"
          :placeholder="placeholder"
          :disabled="isStreaming"
          rows="2"
          @keydown="handleKeydown"
        />
        <button type="submit" :disabled="!inputText.trim() || isStreaming">
          {{ isStreaming ? '回复中' : '发送' }}
        </button>
      </form>
    </section>
  </main>
</template>
