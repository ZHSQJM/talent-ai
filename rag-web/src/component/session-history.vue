<template>
    <div class="session-container" v-loading="loading">
        <!-- 新建会话按钮（可自定义一个新 chatId 或由后端接管） -->
        <el-button type="primary" class="new-btn" @click="handleCreateSession">
            <el-icon>
                <Plus />
            </el-icon>
            新建对话
        </el-button>

        <!-- 会话列表滚动区 -->
        <div class="scroll-list">
            <div v-if="sessionList.length === 0" class="empty-tip">暂无历史会话</div>

            <div v-for="item in sessionList" :key="item.conversation_id" class="session-item"
                :class="{ active: currentChatId === item.conversation_id }" @click="selectSession(item)">
                <div class="session-name" :title="item.conversation_id">
                    {{ item.title || item.conversation_id }}
                </div>
                <div class="meta-row">
                    <span class="time" :title="item.conversation_id">{{ item.last_active_time }}</span>
                    <span class="count-tag">{{ item.message_count }} 条消息</span>
                </div>
            </div>
        </div>

        <!-- 底部状态栏 -->
        <div class="footer-tip">
            <span>共 {{ sessionList.length }} 个活跃会话</span>
            <span class="save-status">● Spring AI Memory</span>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { getConversationsApi, create } from '@/api/chat'

const emit = defineEmits(['select-session', 'update-count'])

const loading = ref(false)
const sessionList = ref([])
const currentChatId = ref(null)

// 1. 获取后端聚合的会话列表
// autoSelect: 是否在还没选中会话时自动选中第一个（新建会话后要选中新会话，所以跳过）
const fetchConversations = async (autoSelect = true) => {
    try {
        loading.value = true
        const res = await getConversationsApi()
        sessionList.value = res || []
        // 把真实会话数同步给父组件，用于顶部标签展示
        emit('update-count', sessionList.value.length)

        // 默认选中第一个
        if (autoSelect && sessionList.value.length > 0 && !currentChatId.value) {
            const first = sessionList.value[0]
            currentChatId.value = first.conversation_id
            emit('select-session', first)
        }
    } catch (error) {
        console.error('获取会话列表失败:', error)
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    fetchConversations()
})

// 2. 选择会话
const selectSession = (item) => {
    currentChatId.value = item.conversation_id
    emit('select-session', item)
}
// 新建会话交互
const handleCreateSession = async () => {
    try {
        // 调用后台 /chat/sessions/new 接口，返回值已在 api 层归一化为 { conversation_id }
        const newSession = await create()

        // 刷新左侧会话列表（后端已把新会话落库，这里能立刻看到它）
        await fetchConversations(false)

        // 选中新建的会话
        if (newSession && newSession.conversation_id) {
            currentChatId.value = newSession.conversation_id
            emit('select-session', newSession)
        }
    } catch (error) {
        console.error('新建会话失败:', error)
    }
}

defineExpose({ fetchConversations })
</script>

<style scoped>
/* 样式与前文保持一致 */
.session-container {
    height: 100%;
    display: flex;
    flex-direction: column;
}

.new-btn {
    width: 100%;
    border-radius: 18px;
    background-color: #009688;
    border-color: #009688;
    margin-bottom: 12px;
}

.scroll-list {
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
}

.empty-tip {
    text-align: center;
    color: #909399;
    font-size: 12px;
    margin-top: 20px;
}

.session-item {
    padding: 10px 12px;
    border-radius: 6px;
    cursor: pointer;
    margin-bottom: 6px;
    background: #fafafa;
    border: 1px solid transparent;
    transition: all 0.2s;
}

.session-item:hover {
    background: #f0f7f6;
}

.session-item.active {
    background: #e6f7f5;
    border-color: #b2dfdb;
}

.session-name {
    font-size: 13px;
    color: #303133;
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin-bottom: 4px;
}

.meta-row {
    display: flex;
    justify-content: space-between;
    font-size: 11px;
    color: #909399;
}

.count-tag {
    background: #eef2ff;
    color: #409eff;
    padding: 0 6px;
    border-radius: 4px;
}

.footer-tip {
    margin-top: auto;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
    color: #909399;
    padding-top: 10px;
    border-top: 1px solid #ebeef5;
}

.save-status {
    color: #67c23a;
}
</style>