<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

import { fetchChatMessages, fetchChatSessions } from '@/api/chats'
import { createChatEventSource } from '@/api/sse'
import ConversationSidebar from '@/components/ConversationSidebar.vue'
import { createChatId } from '@/utils/session'

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
  appType: {
    type: String,
    required: true,
  },
  chatIdPrefix: {
    type: String,
    required: true,
  },
  requestConfig: {
    type: Object,
    required: true,
  },
})

const inputText = ref('')
const messages = ref([])
const conversations = ref([])
const currentChatId = ref('')
const isStreaming = ref(false)
const isLoadingConversations = ref(false)
const isLoadingMessages = ref(false)
const errorMessage = ref('')
const historyError = ref('')
const messageListRef = ref(null)
let activeStream = null

function createWelcomeMessage() {
  return {
    id: 'welcome',
    role: 'assistant',
    content: `你好，我是${props.assistantName}。把你的问题发给我，我们开始吧。`,
  }
}

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

function resetToNewChat() {
  closeActiveStream()
  currentChatId.value = createChatId(props.chatIdPrefix)
  messages.value = [createWelcomeMessage()]
  errorMessage.value = ''
  inputText.value = ''
  isStreaming.value = false
  scrollToBottom()
}

function normalizeHistoryMessages(historyMessages) {
  const normalized = historyMessages
    .filter((message) => message.role === 'user' || message.role === 'assistant')
    .map((message, index) => ({
      id: `${message.role}-${message.order || index}-${currentChatId.value}`,
      role: message.role,
      content: message.content || '',
    }))

  return normalized.length > 0 ? normalized : [createWelcomeMessage()]
}

async function loadConversations(selectInitial = false) {
  isLoadingConversations.value = true
  historyError.value = ''

  try {
    const sessions = await fetchChatSessions(props.appType)
    conversations.value = sessions

    if (selectInitial) {
      if (sessions.length > 0) {
        await selectConversation(sessions[0].chatId)
      } else {
        resetToNewChat()
      }
    }
  } catch (error) {
    historyError.value = '历史记录加载失败'
    if (selectInitial && !currentChatId.value) {
      resetToNewChat()
    }
  } finally {
    isLoadingConversations.value = false
  }
}

async function selectConversation(chatId) {
  if (!chatId) {
    return
  }

  closeActiveStream()
  currentChatId.value = chatId
  errorMessage.value = ''
  inputText.value = ''
  isStreaming.value = false
  isLoadingMessages.value = true

  try {
    const historyMessages = await fetchChatMessages(chatId)
    messages.value = normalizeHistoryMessages(historyMessages)
  } catch (error) {
    errorMessage.value = '历史消息加载失败，请稍后重试。'
    messages.value = [createWelcomeMessage()]
  } finally {
    isLoadingMessages.value = false
    scrollToBottom()
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
  loadConversations(false)
  scrollToBottom()
}

function sendMessage() {
  const message = inputText.value.trim()
  if (!message || isStreaming.value) {
    return
  }

  if (!currentChatId.value) {
    currentChatId.value = createChatId(props.chatIdPrefix)
  }

  closeActiveStream()
  errorMessage.value = ''
  inputText.value = ''

  messages.value = messages.value.filter((item) => item.id !== 'welcome')
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
    props.requestConfig.getParams(message, currentChatId.value),
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

onMounted(() => {
  loadConversations(true)
})

onBeforeUnmount(() => {
  closeActiveStream()
})
</script>

<template>
  <main class="chat-layout">
    <ConversationSidebar
      :app-type="appType"
      :conversations="conversations"
      :current-chat-id="currentChatId"
      :loading="isLoadingConversations"
      :error="historyError"
      @new-chat="resetToNewChat"
      @select-chat="selectConversation"
    />

    <section class="chat-main">
      <header class="chat-header">
        <div class="chat-title">
          <h1>{{ title }}</h1>
          <p>{{ subtitle }}</p>
          <span v-if="currentChatId" class="chat-id">聊天室 ID：{{ currentChatId }}</span>
        </div>
      </header>

      <div ref="messageListRef" class="message-list" aria-live="polite">
        <p v-if="isLoadingMessages" class="message-loading">正在加载历史消息...</p>
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
