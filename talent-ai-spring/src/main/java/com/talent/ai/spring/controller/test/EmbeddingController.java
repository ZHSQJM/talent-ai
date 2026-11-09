//package com.talent.ai.spring.controller.test;
//
//import org.springframework.ai.document.Document;
//import org.springframework.ai.ollama.OllamaEmbeddingModel;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
//import lombok.RequiredArgsConstructor;
//
//import lombok.extern.slf4j.Slf4j;
//
//
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
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
//public class EmbeddingController {
//
//
//
//    private final OllamaEmbeddingModel ollamaEmbeddingModel;
//
//
//    private final VectorStore vectorStore;
//
//
//
//    @RequestMapping("addDoc")
//    public String addDoc(){
//
//        List<Document> docs = List.of(
//                new Document("Milvus是开源向量数据库，支持HNSW索引"),
//                new Document("BGE-M3向量维度1024，适合中文RAG语义检索")
//        );
//        // add 来自 DocumentWriter 父接口，2.0 标准写法
//        vectorStore.add(docs);
//        return "入库成功";
//    }
//
//    @RequestMapping("query")
//    public String query(){
//        List<Document> documents = vectorStore.similaritySearch(
//                SearchRequest.builder()
//                        .query("Milvus是什么")
//                        .topK(2)
//                        .build()
//        );
//        documents.forEach(System.out::println);
//        return "入库成功";
//    }
//
//    @RequestMapping("/em")
//    public String em(){
//        float[] embed = ollamaEmbeddingModel.embed("学java上java1234.com");
//        log.info("{}",Arrays.toString(embed));
//        return Arrays.toString(embed);
//    }
//}
