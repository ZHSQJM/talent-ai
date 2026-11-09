package com.talent.ai.spring.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author watson
 * @date 2026年/11月/05日/13:21
 */
@Configuration
public class TextSplitterConfiguration {



    @Bean
    public TokenTextSplitter tokenTextSplitter(){
        return TokenTextSplitter.builder()
                .withChunkSize(50) //单个文本快最大token数
                .withMaxNumChunks(1000)  //最大可生成的快数量 防止对超长文本无线分割 达到此上线后即使还有剩余文本也停止生产新快
                .build();
    }
}
