package com.talent.ai.spring.controller.test;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * @author watson
 * @date 2026年/11月/04日/15:33
 */

@RestController
@RequestMapping("ollama")
public class OllamaController {


    @Resource
    private  ChatClient ollamaClient;

    @GetMapping("test")
    public String test(@RequestParam("prompt")String prompt){
        return ollamaClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    @GetMapping(value = "stream",produces = "text/html;charset=UTF-8")
    public Flux<String> stream(@RequestParam("prompt")String prompt){
        return ollamaClient.prompt()
                .user(prompt)
                .stream()
                .content();
    }

    @GetMapping(value ="template",produces = "text/html;charset=UTF-8")
    public Flux<String> template(@RequestParam("prompt")String prompt){

        PromptTemplate promptTemplate = new PromptTemplate("介绍下{prompt}");
        Prompt prompt1 = promptTemplate.create(Map.of("prompt", prompt));
        return ollamaClient.prompt(prompt1)
                .stream()
                .content();
    }



    @GetMapping("template2")
    public Flux<String> template2(@RequestParam("prompt")String prompt){

        return ollamaClient.prompt()
                .system("你是一个专业的书评助手")
                .user(u->u.text("请给我推荐三本关于{prompt}的书籍,只需要返回书名").param("prompt", prompt))
                .stream()
                .content();
    }



    @GetMapping("template3")
    public String template3(@RequestParam("prompt")String prompt){

        TopicBook entity = ollamaClient.prompt()
                .system("你是一个专业的书评助手")
                .user(u -> u.text("请给我推荐三本关于{prompt}的书籍,只需要返回书名").param("prompt", prompt))
                .call()
                .entity(TopicBook.class);
        return entity.toString();
    }

    @GetMapping("template4")
    public String template4(@RequestParam("prompt")String prompt){

        List<BookReview> lists = ollamaClient.prompt()
                .system("你是一个专业的书评助手")
                .user(u -> u.text("请给{prompt}的书籍返回3条评价").param("prompt", prompt))
                .call()
                .entity(new ParameterizedTypeReference<List<BookReview>>() {
                });
        return lists.toString();
    }

    @GetMapping(value = "talk",produces = "text/html;charset=UTF-8")
    public Flux<String> talk(@RequestParam("prompt")String prompt,String conversationId){
        return ollamaClient.prompt()
                .user(prompt)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    public record TopicBook(
            String topic, //主题

            List<String> books// 推荐书记
    ){ }


    public record BookReview(
            String reviewerName, //评论人
            int rating,//评分

           String comment// 评论
    ){ }

}
