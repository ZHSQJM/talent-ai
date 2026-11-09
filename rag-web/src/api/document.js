// src/api/document.js
import request from '@/utils/request'
import { SSE_USER_ID } from '@/utils/sse'

/**
 * 上传文档接口
 * @param {FormData} formData 包含 file 的表单数据
 */
export function uploadDocumentApi(formData) {
  // userId 告诉后端把解析进度推给哪条 SSE 连接
  formData.append('userId', SSE_USER_ID)
  return request({
    url: '/upload', // 修改为正确的完整路径
    method: 'post',
    data: formData,
    // 不要手动设置 Content-Type：multipart/form-data 必须带 boundary，
    // 手动设置会丢掉 boundary 导致后端无法解析（Firefox/Safari 尤其明显）。
    // 交由浏览器自动生成即可。
  })
}

/**
 * 获取文件分页列表
 * @param {Object} params 包含 page 和 pageSize
 */
export function getDocumentPageApi(params) {
  return request({
    url: '/file/page', // 对应后端的请求路径
    method: 'get',
    params, // 转换为 ?page=1&pageSize=10 拼接在 URL 后面
  })
}
