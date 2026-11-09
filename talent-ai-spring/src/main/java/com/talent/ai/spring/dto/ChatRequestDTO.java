package com.talent.ai.spring.dto;

import lombok.Data;

/**
 * 发送消息给 AI 的请求体
 *
 * @author watson
 * @date 2026年/11月/06日/15:17
 */
@Data
public class ChatRequestDTO {

    /**
     * 会话 ID，用于在 spring_ai_chat_memory 中区分会话窗口
     */
    private String chatId;

    /**
     * 用户提问内容
     */
    private String message;

    /**
     * 用户ID，用于确定把流式回复推送到哪条 SSE 连接上。
     * 为空时后端使用默认用户（{@link com.talent.ai.spring.service.SseService#DEFAULT_USER_ID}）。
     */
    private String userId;

    /**
     * 单次请求的唯一标识（由前端生成），
     * 后端会在 SSE 事件中原样回传，前端据此把流式片段对应到具体的气泡
     */
    private String requestId;
}
