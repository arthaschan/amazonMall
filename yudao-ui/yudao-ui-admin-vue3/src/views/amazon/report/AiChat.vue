<template>
  <ContentWrap>
    <div class="ai-chat-container" style="height: calc(100vh - 200px); display: flex; flex-direction: column;">
      <!-- 快捷提问按钮 -->
      <div class="quick-actions mb-15px flex flex-wrap gap-10px">
        <el-button
          v-for="q in quickQuestions"
          :key="q"
          size="small"
          round
          @click="sendQuickQuestion(q)"
        >
          {{ q }}
        </el-button>
      </div>

      <!-- 消息列表 -->
      <div
        ref="messageListRef"
        class="message-list flex-1 overflow-y-auto p-15px bg-gray-50 rounded mb-15px"
      >
        <div v-if="chatHistory.length === 0" class="text-center text-gray-400 py-80px">
          <Icon icon="ep:chat-dot-round" class="text-48px mb-10px" />
          <div>你好！我是 AI 运营助手，可以帮你分析店铺数据、优化广告、提供运营建议。</div>
          <div class="mt-5px">请点击上方快捷问题或直接输入你的问题。</div>
        </div>

        <div
          v-for="(msg, index) in chatHistory"
          :key="index"
          class="message-item mb-15px flex"
          :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
        >
          <div
            class="message-bubble max-w-70% p-12px rounded-lg"
            :class="msg.role === 'user'
              ? 'bg-blue-500 text-white'
              : 'bg-white border border-gray-200 text-gray-800'"
          >
            <!-- AI 消息支持 Markdown 渲染 -->
            <div v-if="msg.role === 'assistant'" class="markdown-content" v-html="renderMarkdown(msg.content)" />
            <div v-else class="whitespace-pre-wrap">{{ msg.content }}</div>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="sending" class="message-item mb-15px flex justify-start">
          <div class="message-bubble bg-white border border-gray-200 p-12px rounded-lg">
            <div class="flex items-center gap-5px">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span class="text-gray-500">AI 正在思考中...</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area flex gap-10px">
        <el-input
          v-model="inputMessage"
          placeholder="输入你的问题..."
          @keyup.enter="handleSend"
          :disabled="sending"
          class="flex-1"
        >
          <template #prefix>
            <Icon icon="ep:chat-line-round" />
          </template>
        </el-input>
        <el-button
          type="primary"
          @click="handleSend"
          :disabled="!inputMessage.trim() || sending"
          :loading="sending"
        >
          发送
        </el-button>
      </div>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'
import { AiChatApi } from '@/api/amazon/report'

const message = useMessage()

/** 快捷问题 */
const quickQuestions = [
  '分析本周销售趋势',
  '哪些商品需要补货',
  '优化广告建议',
  '分析差评原因',
  '推荐本周运营动作'
]

/** 聊天历史 */
const chatHistory = ref<Array<{ role: string; content: string }>>([])
const inputMessage = ref('')
const sending = ref(false)
const messageListRef = ref()

/** 加载聊天历史 */
const loadHistory = async () => {
  try {
    const data = await AiChatApi.getAiChatHistory({ limit: 50 })
    chatHistory.value = data || []
    scrollToBottom()
  } catch (error) {
    console.error('加载聊天历史失败', error)
  }
}

/** 发送消息 */
const handleSend = async () => {
  const text = inputMessage.value.trim()
  if (!text || sending.value) return

  // 添加用户消息
  chatHistory.value.push({ role: 'user', content: text })
  inputMessage.value = ''
  scrollToBottom()

  // 发送请求
  sending.value = true
  try {
    const response = await AiChatApi.sendAiChat({ message: text })
    chatHistory.value.push({
      role: 'assistant',
      content: response?.content || response?.message || '抱歉，我暂时无法回答这个问题。'
    })
  } catch (error) {
    chatHistory.value.push({
      role: 'assistant',
      content: '网络异常，请稍后重试。'
    })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

/** 快捷问题 */
const sendQuickQuestion = (question: string) => {
  inputMessage.value = question
  handleSend()
}

/** Markdown 渲染 */
const renderMarkdown = (content: string) => {
  if (!content) return ''
  // 简单的 Markdown 渲染：处理换行、加粗、列表
  return content
    .replace(/\n/g, '<br/>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.*?)`/g, '<code class="bg-gray-100 px-4px py-2px rounded text-sm">$1</code>')
    .replace(/^- (.*?)$/gm, '<li>$1</li>')
    .replace(/(<li>.*?<\/li>)/gs, '<ul class="ml-15px">$1</ul>')
}

/** 滚动到底部 */
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

/** 初始化 */
onMounted(() => {
  loadHistory()
})
</script>
