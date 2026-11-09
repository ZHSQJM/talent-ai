package com.talent.ai.spring.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * @author watson
 * @date 2026年/11月/06日/9:47
 */

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class TlSession {

    private Long id;

    private String userId;

    /**
     * 会话 ID，与 spring_ai_chat_memory.conversation_id 对应
     */
    private String chatId;

    private String title;

    private LocalDateTime createTime;
}
