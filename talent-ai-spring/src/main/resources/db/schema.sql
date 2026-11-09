-- =====================================================================
--  talent-ai 业务库 DDL（MySQL 8.x / utf8mb4）
--
--  用法：
--    mysql -u<user> -p rag < schema.sql
--
--  说明：Spring AI 的聊天记忆表（spring_ai_chat_memory）不在这里，
--  它由 spring.ai.chat.memory.repository.jdbc.initialize-schema=always 自动建表。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 文件表：记录上传到知识库的文档及其解析/向量化状态
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `file_record` (
  `id`          int          NOT NULL AUTO_INCREMENT,
  `file_name`   varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原始文件名',
  `user_id`     varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属用户',
  `size`        int          DEFAULT NULL COMMENT '文件大小（KB）',
  `object_name` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '对象存储中的 key',
  `status`      varchar(20)  NOT NULL DEFAULT 'ready' COMMENT '解析状态：parsing/ready/failed',
  `progress`    int          NOT NULL DEFAULT 0 COMMENT '解析进度百分比 0-100',
  `chunk_count` int          NOT NULL DEFAULT 0 COMMENT '切分后的向量片段数',
  `error_msg`   varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '解析失败原因',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '知识库文档表';

-- ---------------------------------------------------------------------
-- 会话表：用户会话（chat_id 与 spring_ai_chat_memory.conversation_id 对应）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tl_session` (
  `id`          int          NOT NULL AUTO_INCREMENT,
  `user_id`     varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属用户',
  `chat_id`     varchar(64)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话 ID（= conversation_id）',
  `title`       varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话标题',
  `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_chat_id` (`chat_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '会话表';


-- =====================================================================
--  增量迁移（已有库升级用，可重复执行）
-- =====================================================================

-- file_record 增加解析状态相关字段
ALTER TABLE `file_record` ADD COLUMN IF NOT EXISTS `object_name` varchar(500) DEFAULT NULL COMMENT '对象存储中的 key';
ALTER TABLE `file_record` ADD COLUMN IF NOT EXISTS `status`     varchar(20)  NOT NULL DEFAULT 'ready' COMMENT '解析状态：parsing/ready/failed';
ALTER TABLE `file_record` ADD COLUMN IF NOT EXISTS `progress`   int          NOT NULL DEFAULT 0 COMMENT '解析进度百分比 0-100';
ALTER TABLE `file_record` ADD COLUMN IF NOT EXISTS `chunk_count` int         NOT NULL DEFAULT 0 COMMENT '切分后的向量片段数';
ALTER TABLE `file_record` ADD COLUMN IF NOT EXISTS `error_msg`  varchar(1000) DEFAULT NULL COMMENT '解析失败原因';

-- tl_session 增加会话 ID
ALTER TABLE `tl_session` ADD COLUMN IF NOT EXISTS `chat_id` varchar(64) DEFAULT NULL COMMENT '会话 ID（= conversation_id）';

-- 历史上传的文件视为已就绪
UPDATE `file_record` SET `status` = 'ready', `progress` = 100 WHERE `status` IS NULL OR `status` = '';
