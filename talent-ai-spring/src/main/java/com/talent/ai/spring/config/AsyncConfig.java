package com.talent.ai.spring.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置。
 * <p>
 * 文档解析（PDF / Word 抽取文本 + 向量化）是重 IO + 重 CPU 的操作，
 * 单独用一个有界线程池，既不占用 Web 容器的请求线程，也不会因为解析任务堆积
 * 把内存吃光。队列满了走 CallerRunsPolicy，由调用线程兜底执行，保证任务不丢。
 *
 * @author watson
 * @date 2026年/11月/06日/18:40
 */
@Slf4j
@Configuration
public class AsyncConfig {

    /**
     * 文档入库用的线程池，@Async("ragTaskExecutor") 按名字引用
     */
    @Bean("ragTaskExecutor")
    public Executor ragTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("rag-ingest-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
