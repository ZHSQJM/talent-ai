package com.talent.ai.spring.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.talent.ai.spring.dto.ChatStreamEvent;
import com.talent.ai.spring.dto.DocStatusEvent;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author watson
 * @date 2026年/11月/06日/15:24
 */
@Slf4j
@Service
public class SseService {

    /**
     * 连接超时时间(毫秒)，前端在服务端关闭后会自动重连
     */
    private static final long DEFAULT_TIMEOUT = 10 * 60 * 1000L;

    /**
     * 默认用户ID：前端未指定 userId 时使用
     * （与 UploadService 中写死的 file_record.user_id 保持一致）
     */
    public static final String DEFAULT_USER_ID = "1";

    /**
     * userId -> 连接，同一个用户只保留最新一条连接
     */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 建立连接
     *
     * @param userId 用户ID
     * @return SSE 连接
     */
    public SseEmitter subscribe(String userId) {
        // 关闭旧连接，避免同一用户重复连接导致多个 emitter 残留
        close(userId);

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

        try {
            // 建立连接后先发一条，让前端确认连接已就绪
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove(userId, emitter);
        }

        log.info("[sse] 建立连接,userId={}", userId);
        return emitter;
    }

    /**
     * 向指定用户推送消息
     *
     * @param userId 用户ID
     * @param data   推送内容(对象会被序列化为 JSON)
     * @return 是否推送成功(false 表示用户未连接或连接已失效)
     */
    public boolean send(String userId, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.info("用户不存在");
            return false;
        }
        try {
            // SseEmitter 不允许并发发送，锁到 emitter 上保证帧不会交错
            synchronized (emitter) {
                emitter.send(SseEmitter.event().data(data));
            }
            return true;
        } catch (IOException | IllegalStateException e) {
            log.error("[sse] 推送失败,userId={}", userId, e);
            remove(userId, emitter);
            return false;
        }
    }

    /**
     * 推送聊天流式事件（对象会被序列化为 JSON 作为 SSE 帧的 data 部分）
     *
     * @param userId  用户ID
     * @param event   事件内容
     * @return 是否推送成功
     */
    public boolean sendChatEvent(String userId, ChatStreamEvent event) {
        return send(userId, event);
    }

    /**
     * 推送文档解析状态事件
     *
     * @param userId 用户ID
     * @param event  事件内容
     * @return 是否推送成功
     */
    public boolean sendDocStatus(String userId, DocStatusEvent event) {
        return send(userId, event);
    }





    /**
     * 主动断开某个用户的连接
     */
    public void close(String userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
        }
    }



    /**
     * 心跳保活：定期发送空注释帧，避免经网关/代理时因长时间无数据被判定为空闲而断开
     */
    @Scheduled(fixedDelay = 15000)
    public void heartbeat() {
        if (emitters.isEmpty()) {
            return;
        }
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment(""));
            } catch (IOException | IllegalStateException e) {
                remove(userId, emitter);
            }
        });
    }

    private void remove(String userId, SseEmitter emitter) {
        // 仅当当前保存的正是这个 emitter 时才移除，避免误删重连后的新连接
        emitters.remove(userId, emitter);
    }
}
