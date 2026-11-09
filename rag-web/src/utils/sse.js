// src/utils/sse.js
import { fetchEventSource } from '@microsoft/fetch-event-source'

/**
 * SSE 长连接管理（全局单例 + 多订阅者）。
 *
 * 后端提供 GET /sse/subscribe/{userId} 建立长连接，用于服务端主动推送消息
 * （例如文档解析进度、解析完成通知）。这里统一封装：连接的建立、断线重连、
 * 以及事件的分发。业务组件通过 onSseEvent 订阅即可。
 */

// 后端 UploadService 中写死的 userId 为 "1"，此处保持一致
export const SSE_USER_ID = localStorage.getItem('userId') || '1'

const subscribers = new Set()
let controller = null
let manuallyClosed = false

// 把事件分发给所有订阅者
const dispatch = (event) => {
  subscribers.forEach((handler) => {
    try {
      handler(event)
    } catch (e) {
      console.error('[SSE] 订阅回调执行异常:', e)
    }
  })
}

/**
 * 订阅 SSE 事件
 * @param {Function} handler 接收 { event, type, data } 的回调
 * @param {String|Array<String>} [types] 只接收这些 type 的事件，不传则接收全部
 *        type 取自业务报文里的 type 字段，例如 chat-delta / chat-done / chat-error / doc-status
 * @returns {Function} 取消订阅的函数
 */
export function onSseEvent(handler, types) {
  const allowed = types ? (Array.isArray(types) ? types : [types]) : null
  const wrapped = (evt) => {
    if (allowed && !allowed.includes(evt.type)) return
    handler(evt)
  }
  subscribers.add(wrapped)
  return () => subscribers.delete(wrapped)
}

/**
 * 建立 SSE 连接（重复调用幂等，只保留一条连接）
 * @param {String} uid 用户标识
 */
export function connectSse(uid) {
  if (controller) return

  const userId = uid || SSE_USER_ID
  manuallyClosed = false

  controller = new AbortController()
  const signal = controller.signal

  fetchEventSource(`/sse/subscribe/${userId}`, {
    method: 'GET',
    headers: {
      Accept: 'text/event-stream',
      Authorization: 'Bearer ' + (localStorage.getItem('token') || ''),
    },
    signal,
    // 页面切到后台时保持连接
    openWhenHidden: true,
    async onopen(response) {
      if (!response.ok) {
        throw new Error(`[SSE] 连接失败: ${response.status}`)
      }
      const contentType = response.headers.get('content-type') || ''
      if (!contentType.startsWith('text/event-stream')) {
        throw new Error(`[SSE] 响应不是 SSE 流: ${contentType}`)
      }
      console.log('[SSE] 连接已建立')
    },
    onmessage(msg) {
      // 后端建立连接后会先发一条 connected 事件，用于确认通道就绪
      if (msg.event === 'connected') {
        console.log('[SSE] 服务端通道已就绪')
        return
      }
      let data = msg.data
      try {
        data = JSON.parse(msg.data)
      } catch (e) {
        // 非 JSON 报文，原样透传
      }
      // type 优先取业务报文里的 type 字段，取不到则退回 SSE 事件名
      const type = (data && data.type) || msg.event
      dispatch({ event: msg.event, type, data })
    },
    onclose() {
      // 服务端正常关闭后 fetch-event-source 不会再重连，
      // 这里抛错走 onerror 的重连分支
      throw new Error('[SSE] 连接被服务端关闭')
    },
    onerror(err) {
      console.warn('[SSE] 连接异常，将自动重连:', err?.message || err)
      // 不抛出异常 -> fetch-event-source 会按默认间隔自动重试
    },
  }).catch((err) => {
    if (!manuallyClosed) {
      console.error('[SSE] 连接异常终止:', err)
    }
  })
}

/**
 * 断开 SSE 连接
 */
export function disconnectSse() {
  manuallyClosed = true
  if (controller) {
    controller.abort()
    controller = null
  }
}
