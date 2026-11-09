<script setup>
</script>

<template>

</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { connectSse, disconnectSse, SSE_USER_ID } from '@/utils/sse'

const currentSession = ref();

onMounted(()=>{
  connectSse("1");
})


onUnmounted(() => {
    // 组件卸载时断开连接，避免连接泄漏与无意义的自动重连
    disconnectSse()
})

</script>