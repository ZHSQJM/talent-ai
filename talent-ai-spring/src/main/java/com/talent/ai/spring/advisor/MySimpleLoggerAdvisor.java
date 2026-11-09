package com.talent.ai.spring.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

/**
 * @author watson
 * @date 2026年/11月/04日/16:01
 */
@Slf4j
public class MySimpleLoggerAdvisor implements CallAdvisor, StreamAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        log.info("adviseCall");
        return callAdvisorChain.nextCall(chatClientRequest);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        log.info("adviseStream");
        return streamAdvisorChain.nextStream(chatClientRequest)
                .doOnNext(chatClientResponse -> {log.debug("adviseStream");});
    }

    @Override
    public String getName() {
        return "simpleLoggerAdvisor";
    }

    /**
     * 这个getOrder（）方法用于指定advisor的执行顺序
     * 返回值越小 优先级越高 越早执行
     * 返回值0 表示高优先级
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
