package com.talent.ai.spring;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan(basePackages = "com.talent.ai.spring.mapper")
public class TalentAiSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalentAiSpringApplication.class, args);
    }

}
