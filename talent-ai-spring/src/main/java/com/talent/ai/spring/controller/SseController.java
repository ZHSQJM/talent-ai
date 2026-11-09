package com.talent.ai.spring.controller;

import com.talent.ai.spring.service.SseService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author watson
 * @date 2026年/11月/06日/15:23
 */
@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    /**
     * 建立 SSE 长连接，按 userId 区分连接
     */
    @GetMapping("/subscribe/{userId}")
    public SseEmitter connect(@PathVariable("userId") String userId) {
        return sseService.subscribe(userId);
    }

}
