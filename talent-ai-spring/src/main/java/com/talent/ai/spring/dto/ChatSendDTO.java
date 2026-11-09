package com.talent.ai.spring.dto;

import lombok.Data;

/**
 * @author watson
 * @date 2026年/11月/06日/15:17
 */
@Data
public class ChatSendDTO {
    private Integer sessionId;
    private String content;
}