<template>
    <!-- 顶部导航栏 -->
    <div class="header-nav">
        <span class="title">后台管理</span>
        <div class="nav-right">
            <el-button type="text">个人中心</el-button>
            <el-button type="text">退出</el-button>
        </div>
    </div>

    <div class="container">
        <el-row :gutter="20">
            <el-col :span="6">
                <SidebarCard @select-session="handleSessionSelect" />
            </el-col>
            <el-col :span="16">
                <Main :current-session="currentSession" />
            </el-col>
        </el-row>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import SidebarCard from './component/sidebar-panel.vue'
import Main from './component/main.vue'
import { connectSse, disconnectSse, SSE_USER_ID } from '@/utils/sse'

// 1. 定义响应式状态（对应原本的 data）
const currentSession = ref(null)

// 2. 生命周期钩子
onMounted(() => {
    // 建立 SSE 长连接，用于接收服务端推送（如文档解析进度/完成通知）
    connectSse(SSE_USER_ID)
})

onUnmounted(() => {
    // 组件卸载时断开连接，避免连接泄漏与无意义的自动重连
    disconnectSse()
})

// 3. 事件处理函数（对应原本的 methods）
const handleSessionSelect = (session) => {
    currentSession.value = session
}
</script>

<style scoped>
/* 顶部导航 */
.header-nav {
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 60px;
    padding: 0 20px;
    background: #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.title {
    font-size: 18px;
    font-weight: bold;
}

.container {
    margin-top: 50px;
    /* 导航和下方左右布局的间距，可自行修改 16/24/32 */
    /* 水平居中 */
    display: flex;
    justify-content: center;
    /* 垂直居中如果需要就打开 */
    /* align-items: center;
  min-height: 100vh; */
}

/* 限制整行最大宽度，不然会撑很宽 */
.el-row {
    width: 100%;
    max-width: 1600px;
}
</style>