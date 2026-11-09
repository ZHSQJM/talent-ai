package com.talent.ai.spring.controller;

import com.talent.ai.spring.TalentResult;
import com.talent.ai.spring.dto.ChatRequestDTO;
import com.talent.ai.spring.dto.ChatStreamEvent;
import com.talent.ai.spring.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/messages")
public class ChatMessageController {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final SseService sseService;

    /**
     * 获取指定会话的所有历史消息记录
     */
    @GetMapping
    public TalentResult<List<Message>> getChatMessages(@RequestParam String chatId) {
        // 参数 1：会话 ID
        List<Message> messages = chatMemory.get(chatId);
        return TalentResult.success(messages);
    }

    /**
     * 发送消息给 AI，返回 AI 的回答
     * <p>
     * 使用 POST + JSON body，避免长消息放进 URL 超出容器/网关的长度限制。
     * <p>
     * 回答以流式方式生成：每生成一段就通过 SSE 推给前端实时渲染，
     * 同时把完整文本通过接口返回值一并返回，便于前端兜底。
     *
     * @param request 包含 chatId、message、userId、requestId
     * @return AI 完整回答文本
     */
    @PostMapping("/chat")
    public TalentResult<String> chat(@RequestBody ChatRequestDTO request) {
        String chatId = request.getChatId();
        String message = request.getMessage();
        String userId = (request.getUserId() == null || request.getUserId().isBlank())
                ? SseService.DEFAULT_USER_ID
                : request.getUserId();
        String requestId = request.getRequestId();

        StringBuilder fullText = new StringBuilder();
        try {
            this.chatClient.prompt()
                    .user(message)
                    // 必须传入 chatId，用于在 MySQL 中区分不同的用户或会话窗口
                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                    .stream()
                    .content()
                    .doOnNext(chunk -> {
                        if (chunk == null || chunk.isEmpty()) {
                            return;
                        }
                        fullText.append(chunk);
                        // 增量文本实时推给前端
                        sseService.sendChatEvent(userId, ChatStreamEvent.delta(chatId, requestId, chunk));
                    })
                    // 等待流式生成结束
                    .then()
                    .block();

            String content = fullText.toString();
            // 结束事件携带完整回答，前端据此校准最终文本
            sseService.sendChatEvent(userId, ChatStreamEvent.done(chatId, requestId, content));
            return TalentResult.success(content);
        } catch (Exception e) {
            log.error("[chat] 生成回复失败, chatId={}, requestId={}", chatId, requestId, e);
            sseService.sendChatEvent(userId, ChatStreamEvent.error(chatId, requestId, e.getMessage()));
            throw e;
        }
    }
}
