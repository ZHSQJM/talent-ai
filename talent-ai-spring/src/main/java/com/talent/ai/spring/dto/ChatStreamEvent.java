package com.talent.ai.spring.dto;

import lombok.Data;

/**
 * 通过 SSE 推送给前端的聊天流式事件。
 * <p>
 * 序列化后作为 SSE 帧的 data 部分，例如：
 * <pre>
 * data:{"type":"chat-delta","chatId":"abc","requestId":"r1","content":"你好","done":false}
 * </pre>
 *
 * @author watson
 * @date 2026年/11月/06日/16:50
 */
@Data
public class ChatStreamEvent {

    /**
     * 事件类型：
     * chat-delta 增量文本
     * chat-done  生成结束，携带完整回答
     * chat-error 生成异常
     */
    private String type;

    /**
     * 会话 ID
     */
    private String chatId;

    /**
     * 单次请求的唯一标识，前端据此把流式片段对应到具体的气泡，
     * 避免同一会话的并发请求互相串扰
     */
    private String requestId;

    /**
     * 文本内容
     */
    private String content;

    /**
     * 是否为结束事件
     */
    private boolean done;

    public static ChatStreamEvent delta(String chatId, String requestId, String content) {
        ChatStreamEvent event = new ChatStreamEvent();
        event.setType("chat-delta");
        event.setChatId(chatId);
        event.setRequestId(requestId);
        event.setContent(content);
        event.setDone(false);
        return event;
    }

    public static ChatStreamEvent done(String chatId, String requestId, String content) {
        ChatStreamEvent event = new ChatStreamEvent();
        event.setType("chat-done");
        event.setChatId(chatId);
        event.setRequestId(requestId);
        event.setContent(content);
        event.setDone(true);
        return event;
    }

    public static ChatStreamEvent error(String chatId, String requestId, String content) {
        ChatStreamEvent event = new ChatStreamEvent();
        event.setType("chat-error");
        event.setChatId(chatId);
        event.setRequestId(requestId);
        event.setContent(content);
        event.setDone(true);
        return event;
    }
}
