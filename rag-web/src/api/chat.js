// src/api/chat.js
import request from '@/utils/request'
import { SSE_USER_ID } from '@/utils/sse'

/**
 * 统一会话对象结构。
 * 后端两个接口返回的主键字段名不一致：
 * - 会话列表（/chat/sessions/list，来自 SQL 查询）       -> conversation_id
 * - 新建会话（/chat/sessions/new，来自 Map）             -> chatId
 * 这里统一归一化成 conversation_id，前端其余代码只认这一个字段。
 * @param {Object} raw 后端返回的会话对象
 * @returns {Object|null} 归一化后的会话对象
 */
function normalizeSession(raw) {
  if (!raw || typeof raw !== 'object') return null
  const conversationId = raw.conversation_id || raw.chatId || raw.conversationId
  if (!conversationId) return null
  return { ...raw, conversation_id: conversationId }
}

/**
 * 1.创建一个新的会话
 * @returns {Promise<Object|null>} 新会话对象（含 conversation_id）
 */
export function create() {
  return request({
    url: '/chat/sessions/new',
    method: 'get',
    params: { userId: SSE_USER_ID },
  }).then(normalizeSession)
}

/**
 * 1. 获取系统中的所有历史会话列表
 * @returns {Promise<Array>} 会话列表，每项均含 conversation_id
 */
export function getConversationsApi() {
  return request({
    url: '/chat/sessions/list',
    method: 'get',
  }).then((list) => (Array.isArray(list) ? list.map(normalizeSession).filter(Boolean) : []))
}

/**
 * 2. 获取指定会话的历史消息记录
 * @param {String} chatId 会话 ID
 */
export function getChatMessagesApi(chatId) {
  return request({
    url: '/chat/messages',
    method: 'get',
    params: { chatId },
  })
}

/**
 * 3. 发送消息给 AI
 * @param {Object} data 包含 chatId 和 message
 * @returns {Promise<String>} AI 回答文本
 */
export function sendChatMessageApi(data) {
  return request({
    url: '/chat/messages/chat',
    method: 'post',
    data, // JSON body，避免长消息放进 URL 超出容器/网关的长度限制
  })
}
