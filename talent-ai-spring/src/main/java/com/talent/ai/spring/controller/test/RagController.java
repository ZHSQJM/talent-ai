//package com.talent.ai.spring.controller.test;
//
//import com.talent.ai.spring.utils.DocumentParseUtil;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.transformer.splitter.TokenTextSplitter;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
//
///**
// * @author watson
// * @date 2026年/11月/04日/15:33
// */
//
//@RestController
//@RequestMapping("rag")
//@RequiredArgsConstructor
//@Slf4j
//public class RagController {
//
//
//    private final ChatClient ollamaClient;
//
//    private final VectorStore vectorStore;
//
//    private final DocumentParseUtil documentParseUtil;
//
//    private final TokenTextSplitter tokenTextSplitter;
//
//
//    @RequestMapping("addDoc")
//    public String addDoc(){
//        addChunkedDocuments("","");
//        return "ok";
//    }
//
//    @RequestMapping("query")
//    public String query(){
//        QuestionAnswerAdvisor answerAdvisor = QuestionAnswerAdvisor
//                .builder(vectorStore)
//                .searchRequest( SearchRequest.builder()
//                        .topK(3)
//                        .similarityThreshold(0.7) //相似度阈值0-1 值越大要求越严格 过滤低相关性结果 提高RAG贵大指令
//                        .build())
//                        .build();
//
//
//        return ollamaClient.prompt()
//                .advisors(answerAdvisor)
//                .user("Milvus是什么")
//                .call()
//                .content();
//
//    }
//    private void addChunkedDocuments(String path,String label){
//        List<Document> parse = documentParseUtil.parse(path);
//        List<Document> chunks = tokenTextSplitter.apply(parse);
//        vectorStore.add(chunks);
//    }
//}
