//package com.talent.ai.spring.controller;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.ollama.OllamaEmbeddingModel;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.Arrays;
//import java.util.List;
//
//
///**
// * @author watson
// * @date 2026年/11月/04日/15:33
// */
//
//@RestController
//@RequestMapping("ai")
//@RequiredArgsConstructor
//@Slf4j
//public class MyMcpController {
//
//
//
//    private final ChatClient ollamaClient;
//
//
//
//    @RequestMapping("query")
//    public String query(){
//
//        String q = "查一下上海的天气";
//        String content = ollamaClient
//                .prompt()
//                .user(q)
//                .tools()
//                .call()
//                .content();
//
//        return content;
//    }
//
//
//}
