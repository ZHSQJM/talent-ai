package com.talent.ai.spring.controller;

import com.talent.ai.spring.TalentResult;
import com.talent.ai.spring.entity.TlSession;
import com.talent.ai.spring.service.ITlSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author watson
 * @date 2026年/11月/06日/14:41
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/sessions")
public class SessionController {

    /**
     * 默认用户 ID，与前端 localStorage 中的 userId 保持一致
     */
    private static final String DEFAULT_USER_ID = "1";

    private final ITlSessionService tlSessionService;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 点击“新建对话”时调用，返回一个新的 chatId，并落库登记，
     * 这样新会话在第一次发消息之前就能出现在左侧列表里。
     */
    @GetMapping("/new")
    public Map<String, String> createNewSession(@RequestParam(value = "userId", required = false) String userId) {
        String newChatId = UUID.randomUUID().toString();
        String owner = (userId == null || userId.isBlank()) ? DEFAULT_USER_ID : userId;

        TlSession session = new TlSession()
                .setUserId(owner)
                .setChatId(newChatId)
                .setTitle("新对话")
                .setCreateTime(LocalDateTime.now());
        tlSessionService.save(session);

        Map<String, String> result = new HashMap<>();
        result.put("chatId", newChatId);
        // 同时给出 conversation_id，前端两种命名都能用
        result.put("conversation_id", newChatId);
        return result;
    }

    /**
     * 获取会话列表。
     * <p>
     * 会话可能来自两个地方：
     * <ul>
     *   <li>{@code tl_session}：点击“新建对话”时登记的空会话（还没有任何消息）；</li>
     *   <li>{@code spring_ai_chat_memory}：真正产生过消息的会话。</li>
     * </ul>
     * 两者取并集，缺失的一侧用 0 条消息 / 创建时间补齐，最后按最后活跃时间倒序。
     */
    @GetMapping("/list")
    public TalentResult<List<Map<String, Object>>> listSessions() {
        // 注意：timestamp 是 MySQL 保留字，必须用反引号包裹，否则语法错误
        String sql = """
                SELECT s.chat_id AS conversation_id,
                       COALESCE(m.last_active_time, s.create_time) AS last_active_time,
                       COALESCE(m.message_count, 0) AS message_count,
                       COALESCE(
                           (SELECT SUBSTRING(mm.content, 1, 30)
                              FROM spring_ai_chat_memory mm
                             WHERE mm.conversation_id = s.chat_id AND mm.type = 'USER'
                             ORDER BY mm.sequence_id ASC LIMIT 1),
                           s.title,
                           '新对话'
                       ) AS title
                FROM tl_session s
                LEFT JOIN (
                    SELECT conversation_id,
                           MAX(`timestamp`) AS last_active_time,
                           COUNT(*) AS message_count
                    FROM spring_ai_chat_memory
                    GROUP BY conversation_id
                ) m ON m.conversation_id = s.chat_id
                WHERE s.chat_id IS NOT NULL

                UNION

                SELECT m2.conversation_id,
                       m2.last_active_time,
                       m2.message_count,
                       COALESCE(
                           (SELECT SUBSTRING(mm.content, 1, 30)
                              FROM spring_ai_chat_memory mm
                             WHERE mm.conversation_id = m2.conversation_id AND mm.type = 'USER'
                             ORDER BY mm.sequence_id ASC LIMIT 1),
                           '历史会话'
                       )
                FROM (
                    SELECT conversation_id,
                           MAX(`timestamp`) AS last_active_time,
                           COUNT(*) AS message_count
                    FROM spring_ai_chat_memory
                    GROUP BY conversation_id
                ) m2
                WHERE m2.conversation_id NOT IN (SELECT chat_id FROM tl_session WHERE chat_id IS NOT NULL)

                ORDER BY last_active_time DESC
                """;

        return TalentResult.success(jdbcTemplate.queryForList(sql));
    }
}
