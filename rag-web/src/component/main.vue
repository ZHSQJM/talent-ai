<template>
    <div class="chat-wrap" v-loading="loading">
        <!-- 顶部标题栏 -->
        <div class="chat-top-bar">
            <div class="title-line">
                <span class="chat-title">
                    当前会话: {{ currentSession?.title || currentSession?.conversation_id || '未选择' }}
                </span>
                <span v-if="currentSession?.conversation_id" class="session-id">
                    {{ currentSession.conversation_id }}
                </span>
            </div>
        </div>

        <!-- 聊天消息滚动区 -->
        <div class="chat-scroll" ref="scrollContainer">
            <div v-if="!currentSession" class="empty-tip">请从左侧选择一个会话</div>
            <div v-else-if="messageList.length === 0" class="empty-tip">开始向 AI 提问吧...</div>

            <!-- Spring AI 返回的 Message 结构渲染 -->
            <template v-for="(msg, index) in messageList" :key="index">
                <!-- 用户提问 -->
                <div v-if="isUserMessage(msg)" class="user-bubble-wrapper">
                    <div class="user-bubble">
                        {{ msg.content || msg.text }}
                    </div>
                </div>

                <!-- 发送失败提示 -->
                <div v-else-if="msg.failed" class="ai-block">
                    <div class="ai-card error-card">
                        <p class="ai-intro">{{ msg.content || msg.text }}</p>
                    </div>
                </div>

                <!-- AI 回答 -->
                <div v-else class="ai-block">
                    <div class="ai-card">
                        <p class="ai-intro">{{ msg.content || msg.text }}</p>
                    </div>
                </div>
            </template>

            <!-- AI 正在思考（开始流式输出后隐藏） -->
            <div v-if="sending && !streamedText" class="ai-block">
                <div class="ai-card">
                    <p class="ai-intro ai-typing">
                        <el-icon class="is-loading">
                            <Loading />
                        </el-icon>
                        AI 正在思考...
                    </p>
                </div>
            </div>
        </div>

        <!-- 底部输入栏 -->
        <div class="chat-bottom">
            <div class="input-box-wrapper">
                <el-input v-model="queryText" type="textarea" :rows="2" placeholder="输入消息，Enter 发送..."
                    class="input-area" resize="none" @keyup.enter.exact.prevent="handleSend" />
                <div class="bottom-bar-bottom">
                    <span class="rag-tip">● Spring AI Memory 已启用</span>
                    <el-button circle type="primary" class="send-btn" :loading="sending"
                        :disabled="!queryText.trim() || !currentSession || sending" @click="handleSend">
                        <el-icon>
                            <Promotion />
                        </el-icon>
                    </el-button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, Loading } from '@element-plus/icons-vue'
import { getChatMessagesApi, sendChatMessageApi } from '@/api/chat'
import { onSseEvent, SSE_USER_ID } from '@/utils/sse'

const props = defineProps({
    currentSession: {
        type: Object,
        default: () => null
    }
})

const loading = ref(false) // 正在拉取历史消息
const sending = ref(false) // 正在等待 AI 回答
const queryText = ref('')
const messageList = ref([])
const scrollContainer = ref(null)

// 当前流式请求的状态：
// { chatId, requestId, index, finished }
// finished 表示内容已完整（HTTP 响应已到或已收到 chat-done），此后的 SSE 片段一律忽略
const pendingStream = ref(null)

// 竞态控制：每次请求自增序号，只有序号等于最新值的结果才允许写入列表，
// 避免快速切换会话时旧请求的响应覆盖新会话的数据
let requestSeq = 0
let unsubscribe = null

// 兼容 Spring AI 的 messageType（USER/ASSISTANT）与后端可能的 role（user/assistant）
const isUserMessage = (msg) => {
    const type = String(msg.messageType || msg.role || '').toLowerCase()
    return type === 'user'
}

// 已经开始流式输出的文本，有内容后就不再显示"AI 正在思考"
const streamedText = computed(() => {
    const target = pendingStream.value
    if (!target) return ''
    const msg = messageList.value[target.index]
    return (msg && msg.content) || ''
})

// 订阅 SSE：接收后端边生成边推送的回复片段
onMounted(() => {
    unsubscribe = onSseEvent((evt) => {
        const data = evt.data || {}
        const target = pendingStream.value
        if (!target) return

        // 优先用 requestId 精确匹配，避免同一会话的并发请求互相串扰
        if (data.requestId) {
            if (data.requestId !== target.requestId) return
        } else if (data.chatId !== target.chatId) {
            return
        }

        // 接口返回的完整回答已就位，忽略迟到的片段，防止内容重复
        if (target.finished) return

        const msg = messageList.value[target.index]
        if (!msg) return

        if (evt.type === 'chat-delta') {
            msg.content = (msg.content || '') + (data.content || '')
        } else if (evt.type === 'chat-done') {
            // 用完整回答校准最终文本，避免增量拼接遗漏
            if (data.content) msg.content = data.content
            target.finished = true
        } else if (evt.type === 'chat-error') {
            msg.content = msg.content || data.content || 'AI 生成回复时出错'
            msg.failed = true
            target.finished = true
        }
        scrollToBottom()
    }, ['chat-delta', 'chat-done', 'chat-error'])
})

onUnmounted(() => {
    unsubscribe?.()
})

// 监听会话切换加载历史消息
watch(() => props.currentSession, async (newVal) => {
    if (newVal && newVal.conversation_id) {
        await fetchMessages(newVal.conversation_id)
    } else {
        requestSeq++ // 作废进行中的请求
        messageList.value = []
    }
}, { immediate: true })

const fetchMessages = async (chatId) => {
    const seq = ++requestSeq
    try {
        loading.value = true
        const res = await getChatMessagesApi(chatId)
        // 期间又发起了新的请求，本次结果已过期，直接丢弃
        if (seq !== requestSeq) return
        messageList.value = res || []
        scrollToBottom()
    } catch (error) {
        if (seq === requestSeq) {
            console.error('获取历史消息失败:', error)
        }
    } finally {
        if (seq === requestSeq) {
            loading.value = false
        }
    }
}

// 发送消息
const handleSend = async () => {
    const content = queryText.value.trim()
    if (!content || !props.currentSession || sending.value) return

    const chatId = props.currentSession.conversation_id
    // 单次请求标识，后端会在 SSE 事件里原样回传，用于把片段对应到本气泡
    const requestId = `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`

    // 1. 界面立即追加用户消息
    messageList.value.push({
        messageType: 'USER',
        content: content
    })
    queryText.value = ''

    // 2. 预置一个空的 AI 气泡，SSE 推送的增量会往这里追加
    messageList.value.push({ messageType: 'ASSISTANT', content: '' })
    // 取回响应式代理（直接改 push 进去的原始对象不会触发视图更新）
    const aiIndex = messageList.value.length - 1
    const aiMessage = messageList.value[aiIndex]
    pendingStream.value = {
        chatId,
        requestId,
        index: aiIndex,
        finished: false
    }

    sending.value = true
    scrollToBottom()

    try {
        // 3. 调用后端 /chat/messages/chat，回复会边生成边通过 SSE 推回来
        const aiResponseText = await sendChatMessageApi({
            chatId: chatId,
            message: content,
            userId: SSE_USER_ID,
            requestId: requestId
        })

        // 4. 若 SSE 还没给出完整内容（例如 SSE 未连接），用接口返回值兜底
        const target = pendingStream.value
        if (target && target.requestId === requestId && !target.finished) {
            aiMessage.content = aiResponseText || aiMessage.content
            target.finished = true
        }
        scrollToBottom()
    } catch (error) {
        console.error('发送消息失败:', error)
        ElMessage.error('消息发送失败，请稍后重试')
        aiMessage.content = aiMessage.content || '（消息发送失败，请重试）'
        aiMessage.failed = true
        const target = pendingStream.value
        if (target && target.requestId === requestId) {
            target.finished = true
        }
        scrollToBottom()
    } finally {
        sending.value = false
    }
}

const scrollToBottom = () => {
    nextTick(() => {
        if (scrollContainer.value) {
            scrollContainer.value.scrollTop = scrollContainer.value.scrollHeight
        }
    })
}
</script>

<style scoped>
.chat-wrap {
    height: 100%;
    display: flex;
    flex-direction: column;
    background: #fff;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid #e4e7ed;
}

.chat-top-bar {
    padding: 14px 20px;
    border-bottom: 1px solid #ebeef5;
    background: #fafbfc;
}

.title-line {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    display: flex;
    align-items: center;
    gap: 10px;
}

.session-id {
    font-size: 11px;
    font-weight: 400;
    color: #909399;
}

.chat-scroll {
    flex: 1;
    padding: 20px;
    overflow-y: auto;
}

.empty-tip {
    text-align: center;
    color: #909399;
    font-size: 13px;
    margin-top: 60px;
}

.user-bubble-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 16px;
}

.user-bubble {
    background-color: #f0f7f6;
    color: #2c3e50;
    padding: 10px 14px;
    border-radius: 10px 10px 2px 10px;
    max-width: 75%;
    font-size: 14px;
    border: 1px solid #d1e7e4;
}

.ai-block {
    margin-bottom: 16px;
}

.ai-card {
    background: #f9fbfb;
    border: 1px solid #e8f0ef;
    border-radius: 10px;
    padding: 14px;
    max-width: 85%;
}

.ai-intro {
    font-size: 14px;
    color: #303133;
    line-height: 1.5;
    margin: 0;
    white-space: pre-wrap;
}

.ai-card.error-card {
    background: #fef0f0;
    border-color: #fbc4c4;
}

.ai-card.error-card .ai-intro {
    color: #f56c6c;
}

.ai-typing {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #909399 !important;
}

.chat-bottom {
    padding: 14px 20px;
    border-top: 1px solid #ebeef5;
    background: #fafbfc;
}

.input-box-wrapper {
    background: #fff;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    padding: 8px 12px;
}

.bottom-bar-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 6px;
}

.rag-tip {
    font-size: 12px;
    color: #909399;
}

.send-btn {
    background-color: #009688;
    border-color: #009688;
}

:deep(.input-area .el-textarea__inner) {
    box-shadow: none !important;
    border: none !important;
    padding: 0;
    font-size: 14px;
}
</style>