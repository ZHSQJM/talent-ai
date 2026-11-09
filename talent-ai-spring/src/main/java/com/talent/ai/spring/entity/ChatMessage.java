package com.talent.ai.spring.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author watson
 * @date 2026年/11月/06日/15:21
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    private Long id;

    private Long sessionId;

    private Integer type;

    private String content;

    private LocalDateTime createTime;
}
