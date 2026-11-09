package com.talent.ai.spring.config;

import com.talent.ai.spring.tool.DateTimeTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author watson
 * @date 2026年/11月/04日/15:15
 */

@Configuration
@RequiredArgsConstructor
public class AiConfiguration {


    private final DateTimeTools dateTimeTools;

    /**
     * 检索召回的片段数
     */
    @Value("${rag.advisor.top-k:5}")
    private int topK;

    /**
     * 相似度阈值，低于该值的片段不参与回答
     */
    @Value("${rag.advisor.similarity-threshold:0.5}")
    private double similarityThreshold;

    String systemPrompt = """
                你是一个专业的后台管理的操作助手，
                可以帮登录用户创建任务，查询资料以及统计数据，
                如果自己不确定是否能够操作直接回答“关于这个问题，我目前没有确切的信息”，禁止编造内容
                """;

    /**
     * RAG 问答模板。
     * <p>
     * 相比 Spring AI 默认模板，这里做了两点调整：
     * <ul>
     *   <li>明确「参考资料为空时按自身知识正常回答」，避免知识库里还没传文档时
     *       模型只会拒答；</li>
     *   <li>要求标注引用来源，方便用户溯源。</li>
     * </ul>
     * 占位符 {query} 与 {question_answer_context} 是 Spring AI 约定的名字，不能改。
     */
    private static final String RAG_PROMPT_TEMPLATE = """
            {query}

            以下是从知识库中检索到的参考资料（为空表示知识库中暂时没有相关内容）：
            ---------------------
            {question_answer_context}
            ---------------------

            回答要求：
            1. 如果参考资料中有相关内容，请优先依据参考资料回答，并在答案末尾用「参考来源：文件名」标注引用了哪些文档；
            2. 如果参考资料为空或其中没有答案，请直接依据你自己的知识正常回答用户，不要因为参考资料为空而拒答；
            3. 禁止编造参考资料中不存在的内容。
            """;


    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel, ChatMemory chatMemory,VectorStore vectorStore) {


        return ChatClient.builder(openAiChatModel)
                //.defaultAdvisors(new MySimpleLoggerAdvisor())
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build(), //添加有一个聊天记录

                        // 把向量库召回的片段拼进用户消息。
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .topK(topK)
                                        .similarityThreshold(similarityThreshold)
                                        .build())
                                .promptTemplate(new PromptTemplate(RAG_PROMPT_TEMPLATE))
                                .build())
                .defaultSystem(systemPrompt)
                .defaultTools(dateTimeTools)
                .build();
    }
//
//    @Bean
//    public ChatClient ollamaClient(OllamaChatModel ollamaChatModel, ChatMemory chatMemory, VectorStore vectorStore) {
//        return ChatClient.builder(ollamaChatModel)
//                .defaultAdvisors(new SimpleLoggerAdvisor(),
//                        // 聊天记忆：order = HIGHEST_PRECEDENCE + 200，最先执行。
//                        // 它在 before() 里就把「原始用户问题」写进记忆，
//                        // 因此后面 RAG 增强过的 prompt 不会被存进记忆里。
//                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
//                        // RAG 检索：order = 0，在记忆之后、调用模型之前执行，
//                        // 把向量库召回的片段拼进用户消息。
//                        QuestionAnswerAdvisor.builder(vectorStore)
//                                .searchRequest(SearchRequest.builder()
//                                        .topK(topK)
//                                        .similarityThreshold(similarityThreshold)
//                                        .build())
//                                .promptTemplate(new PromptTemplate(RAG_PROMPT_TEMPLATE))
//                                .build())
//                .defaultSystem(systemPrompt)
//                .defaultTools(dateTimeTools)
//                .build();
//    }
}
