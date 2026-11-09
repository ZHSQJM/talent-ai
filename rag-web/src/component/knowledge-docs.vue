<template>
    <div class="docs-container" v-loading="loading">
        <!-- 标题与状态栏 -->
        <div class="section-header">
            <div class="header-title">
                <el-icon class="shield-icon">
                    <CircleCheck />
                </el-icon>
                <span>文档检索源</span>
            </div>
            <div class="selected-badge">
                已选 {{ selectedCount }} / 共 {{ totalCount }} 篇
            </div>
        </div>

        <!-- 上传新文档区域 -->
        <el-upload class="upload-wrap" action="#" :http-request="handleCustomUpload" :show-file-list="false"
            :before-upload="beforeUpload">
            <div class="upload-dropzone">
                <div class="upload-content">
                    <el-icon class="upload-icon">
                        <Plus />
                    </el-icon>
                    <span class="upload-text">上传新文档</span>
                </div>
                <div class="upload-hint">支持 PDF, Word, Markdown · 最大 50MB</div>
            </div>
        </el-upload>

        <!-- 文档列表区域 -->
        <div class="scroll-list">
            <div v-for="doc in documentList" :key="doc.id" class="doc-item"
                :class="{ 'is-parsing': doc.status === 'parsing', 'is-failed': doc.status === 'failed' }">
                <el-checkbox v-model="doc.selected" :disabled="doc.status === 'parsing'" class="doc-checkbox"
                    @change="emitUpdate" />

                <div class="doc-info">
                    <div class="doc-name-row">
                        <el-icon class="file-type-icon">
                            <Document />
                        </el-icon>
                        <span class="doc-name" :title="doc.fileName">{{ doc.fileName }}</span>
                    </div>
                    <div class="doc-meta">
                        <span class="size">{{ formatSize(doc.size) }}</span>
                        <span class="dot">·</span>
                        <template v-if="doc.status === 'parsing'">
                            <span class="status-parsing">正在解析中 {{ doc.progress || 0 }}%...</span>
                        </template>
                        <template v-else-if="doc.status === 'failed'">
                            <span class="status-failed" :title="doc.errorMsg || '解析失败'">解析失败</span>
                        </template>
                        <template v-else>
                            <span class="status-ready">就绪</span>
                            <template v-if="doc.chunkCount">
                                <span class="dot">·</span>
                                <span>{{ doc.chunkCount }} 个片段</span>
                            </template>
                        </template>
                    </div>
                    <!-- 解析进度条 -->
                    <div v-if="doc.status === 'parsing'" class="progress-bar-wrap">
                        <div class="progress-bar-inner" :style="{ width: (doc.progress || 0) + '%' }"></div>
                    </div>
                    <!-- 失败原因 -->
                    <div v-if="doc.status === 'failed' && doc.errorMsg" class="error-tip">{{ doc.errorMsg }}</div>
                </div>
            </div>
        </div>

        <!-- 底部提示栏 -->
        <div class="footer-tip">
            <el-icon class="tip-icon">
                <InfoFilled />
            </el-icon>
            <span>已激活文档将作为 AI 问答参考源 <span class="highlight-action">(勾选即检索)</span></span>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, CircleCheck, Document, InfoFilled } from '@element-plus/icons-vue'
import { uploadDocumentApi, getDocumentPageApi } from '@/api/document'
import { onSseEvent } from '@/utils/sse'

const emit = defineEmits(['update-counts'])

const loading = ref(false)
const documentList = ref([])
const pagination = ref({
    currentPage: 1,
    pageSize: 10,
    total: 0
})

// SSE 订阅的取消函数
let unsubscribe = null

const totalCount = computed(() => pagination.value.total || documentList.value.length)
const selectedCount = computed(() => documentList.value.filter(item => item.selected).length)

// 后端 FileRecord.size 存的是 KB（UploadService 中做了 size/1024）
const formatSize = (kb) => {
    if (kb === null || kb === undefined) return '-'
    if (kb < 1024) return `${kb} KB`
    return `${(kb / 1024).toFixed(1)} MB`
}

// 1. 页面挂载时请求后端分页列表
const fetchDocumentList = async () => {
    try {
        loading.value = true
        const res = await getDocumentPageApi({
            page: pagination.value.currentPage,
            pageSize: pagination.value.pageSize
        })

        // 兼容后端返回的实体格式（MyBatis-Plus 的 IPage 结构：records 为列表，total 为总数）
        const records = res?.records || res?.list || (Array.isArray(res) ? res : [])
        documentList.value = records.map(item => ({
            ...item,
            // 后端目前没有 selected 字段，默认不勾选，由用户显式选择检索源
            selected: item.selected ?? false
        }))
        pagination.value.total = res?.total || records.length

        emitUpdate()
    } catch (error) {
        console.error('获取文档列表失败:', error)
    } finally {
        loading.value = false
    }
}

// 2. 订阅后端推送的解析进度事件
const handleDocStatus = (evt) => {
    const data = evt.data || {}
    if (!data.fileId) return

    const doc = documentList.value.find(item => String(item.id) === String(data.fileId))
    if (!doc) {
        // 新上传的文件还不在当前页（或已翻页），拉一次列表
        fetchDocumentList()
        return
    }

    // 就地更新，不重新请求整个列表（进度事件比较频繁）
    doc.status = data.status
    doc.progress = data.progress
    if (data.chunkCount !== null && data.chunkCount !== undefined) {
        doc.chunkCount = data.chunkCount
    }
    doc.errorMsg = data.message || null

    // 解析结束（成功或失败）时拉一次最终数据，确保与库里的记录一致
    if (data.status === 'ready' || data.status === 'failed') {
        fetchDocumentList()
    }
}

onMounted(() => {
    fetchDocumentList()
    // 只订阅文档相关事件，收到通知后更新进度
    // （聊天事件的 type 是 chat-*，这里按类型过滤掉，避免每收到一个 token 就刷新一次）
    unsubscribe = onSseEvent(handleDocStatus, 'doc-status')
})

onUnmounted(() => {
    unsubscribe?.()
})

const emitUpdate = () => {
    emit('update-counts', { selected: selectedCount.value, total: totalCount.value })
}

// 文件上传前校验（限制 50MB）
const beforeUpload = (file) => {
    const isLt50M = file.size / 1024 / 1024 < 50
    if (!isLt50M) {
        ElMessage.error('上传文档大小不能超过 50MB!')
        return false
    }
    return true
}

// 3. 对接后端上传接口
const handleCustomUpload = async (options) => {
    const { file } = options

    const formData = new FormData()
    formData.append('file', file)
    try {
        await uploadDocumentApi(formData)
        ElMessage.success('文档上传成功，正在后台解析...')

        // 上传成功后重新刷新列表（此时状态为「解析中」）
        fetchDocumentList()
    } catch (error) {
        console.error('文件上传失败:', error)
    }
}

defineExpose({ selectedCount, totalCount })
</script>

<style scoped>
/* 样式与前文保持一致 */
.docs-container {
    height: 100%;
    display: flex;
    flex-direction: column;
}

.upload-wrap {
    display: block;
    width: 100%;
    margin-bottom: 12px;
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
}

.header-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 600;
    color: #303133;
}

.shield-icon {
    color: #009688;
    font-size: 15px;
}

.selected-badge {
    font-size: 11px;
    background: #e6f7f5;
    color: #009688;
    padding: 2px 8px;
    border-radius: 12px;
    font-weight: 500;
}

.upload-dropzone {
    border: 1.5px dashed #b2dfdb;
    background: #f4faf9;
    border-radius: 8px;
    padding: 12px;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s;
}

.upload-dropzone:hover {
    background: #e6f7f5;
}

.upload-content {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 2px;
}

.upload-hint {
    font-size: 11px;
    color: #909399;
}

.scroll-list {
    flex: 1;
    overflow-y: auto;
    padding-right: 2px;
    display: flex;
    flex-direction: column;
}

.doc-item {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 8px 10px;
    background: #fafafa;
    border: 1px solid #ebeef5;
    border-radius: 6px;
    margin-bottom: 8px;
    transition: background 0.2s;
}

.doc-item:hover {
    background: #f5f7fa;
}

.doc-item.is-parsing {
    background: #fdfdfd;
    border-style: dashed;
}

.doc-item.is-failed {
    background: #fef7f7;
    border-color: #fbc4c4;
}

.doc-checkbox {
    margin-top: 2px;
}

.doc-info {
    flex: 1;
    min-width: 0;
}

.doc-name-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 2px;
}

.file-type-icon {
    color: #409eff;
    font-size: 14px;
    flex-shrink: 0;
}

.doc-name {
    font-size: 13px;
    font-weight: 500;
    color: #303133;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.doc-meta {
    font-size: 11px;
    color: #909399;
    display: flex;
    align-items: center;
    gap: 4px;
}

.status-ready {
    color: #67c23a;
    font-weight: 500;
}

.status-parsing {
    color: #e6a23c;
    font-weight: 500;
}

.status-failed {
    color: #f56c6c;
    font-weight: 500;
    cursor: help;
}

.error-tip {
    margin-top: 4px;
    font-size: 11px;
    color: #f56c6c;
    line-height: 1.4;
    word-break: break-all;
}

.progress-bar-wrap {
    width: 100%;
    height: 4px;
    background: #ebeef5;
    border-radius: 2px;
    margin-top: 4px;
    overflow: hidden;
}

.progress-bar-inner {
    height: 100%;
    background: #009688;
    transition: width 0.3s ease;
}

.footer-tip {
    margin-top: auto;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    font-size: 12px;
    color: #909399;
    padding-top: 10px;
    border-top: 1px solid #ebeef5;
}

.tip-icon {
    color: #909399;
}

.highlight-action {
    color: #009688;
    font-weight: 500;
}
</style>
