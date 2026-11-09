<template>
    <el-card class="sidebar-card"
        body-style="padding: 12px; height: 100%; display: flex; flex-direction: column; box-sizing: border-box;">
        <!-- 顶部 Tab 标签切换 -->
        <div class="tab-wrap">
            <div class="tab-item" :class="{ active: currentTab === 'docs' }" @click="currentTab = 'docs'">
                知识文档 <span class="num">{{ docCounts.selected }}/{{ docCounts.total }}</span>
            </div>
            <div class="tab-item" :class="{ active: currentTab === 'history' }" @click="currentTab = 'history'">
                历史会话 <span class="num">{{ sessionCount }}</span>
            </div>
        </div>

        <!-- 两个面板都保持挂载（用 v-show 隐藏），
             这样切换标签时不会重新请求数据，顶栏的会话数/文档数也始终是最新的 -->
        <div class="panel-content">
            <KnowledgeDocs v-show="currentTab === 'docs'" @update-counts="handleDocCountsUpdate" />
            <SessionHistory v-show="currentTab === 'history'" @select-session="handleSessionSelect"
                @update-count="handleSessionCountUpdate" />
        </div>
    </el-card>
</template>

<script setup>
import { ref } from 'vue'
import KnowledgeDocs from './knowledge-docs.vue'
import SessionHistory from './session-history.vue'

const currentTab = ref('docs')

// 向上抛出选中的会话，由 App.vue 传给 Main 组件
const emit = defineEmits(['update-counts', 'select-session'])

// 维护顶栏状态指示
const docCounts = ref({ selected: 0, total: 0 })
const sessionCount = ref(0)

const handleDocCountsUpdate = ({ selected, total }) => {
    docCounts.value = { selected, total }
}

const handleSessionCountUpdate = (count) => {
    sessionCount.value = count
}

const handleSessionSelect = (session) => {
    // 继续向上传递给 App.vue
    emit('select-session', session)
}
</script>

<style scoped>
.sidebar-card {
    height: 100%;
    border-radius: 8px;
    border: 1px solid #e4e7ed;
}

:deep(.el-card__body) {
    height: 100%;
    box-sizing: border-box;
}

.tab-wrap {
    display: flex;
    gap: 8px;
    margin-bottom: 12px;
}

.tab-item {
    flex: 1;
    text-align: center;
    padding: 6px 0;
    border-radius: 6px;
    background: #f4f5f7;
    font-size: 13px;
    color: #606266;
    cursor: pointer;
    transition: all 0.2s ease;
    user-select: none;
}

.tab-item.active {
    background: #e6f7f5;
    color: #009688;
    font-weight: 600;
}

.num {
    margin-left: 2px;
    font-size: 12px;
    opacity: 0.85;
}

.panel-content {
    flex: 1;
    overflow: hidden;
}
</style>