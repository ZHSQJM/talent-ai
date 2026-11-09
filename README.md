# talent-ai

基于 Spring AI 的 RAG 知识库问答应用。

- `talent-ai-spring`：Spring Boot 4.1.2 / Spring AI 2.0.1 后端
- `rag-web`：Vue 3 + Vite 前端
- `spring-ai-LangChain4j`、`talent-ai-mcp`：实验性模块

## 环境要求

| 组件 | 版本 |
| --- | --- |
| JDK | **21**（Spring Boot 4 最低要求 Java 17，项目按 21 编译） |
| MySQL | 8.x（默认库名 `rag`） |
| Milvus | 2.x（向量库，默认 `127.0.0.1:19530`） |
| Ollama | 需提前 `ollama pull` 对应模型 |
| MinIO | 存放上传的原始文件 |
| Node.js | 18+（前端构建） |

> ⚠️ 本机 `JAVA_HOME` 若指向 JDK 8，命令行直接跑 `mvn` 会报
> `unclosed string literal`（JDK 8 不支持文本块 `"""`）。
> 构建前请切到 JDK 21：
>
> ```bash
> # Windows
> set JAVA_HOME=C:\Program Files\Java\jdk-21.0.12.1
> # macOS / Linux
> export JAVA_HOME=/usr/lib/jvm/jdk-21
> ```

## 密钥配置

仓库里**不存放任何真实密钥**。`application.yml` 中全部使用占位符：

```yaml
spring:
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY:}
  datasource:
    password: ${MYSQL_PASSWORD:}
minio:
  accessKey: ${MINIO_ACCESS_KEY:}
  secretKey: ${MINIO_SECRET_KEY:}
```

真实值有两种提供方式（环境变量优先级更高）：

1. **环境变量**：直接设置 `DEEPSEEK_API_KEY`、`MYSQL_PASSWORD`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY` 等；
2. **本地配置文件**：复制模板并填写实际值

   ```bash
   cp talent-ai-spring/src/main/resources/application-local.yml.example \
      talent-ai-spring/src/main/resources/application-local.yml
   ```

   `application-local.yml` 已加入 `.gitignore`（**禁止提交**），并通过
   `spring.profiles.include: local` 自动加载，profile 专属配置的优先级高于
   `application.yml`。

> 之前曾以明文提交过的 DeepSeek key / MySQL root 密码 / MinIO AK-SK，**请尽快轮换**。

## 数据库

建表脚本见 `talent-ai-spring/src/main/resources/db/schema.sql`：

```bash
mysql -u root -p rag < talent-ai-spring/src/main/resources/db/schema.sql
```

脚本是幂等的，已包含 `file_record` 状态字段与 `tl_session.chat_id` 的增量迁移。

## 启动

```bash
# 后端
mvn -pl talent-ai-spring spring-boot:run

# 前端
cd rag-web && npm install && npm run dev
```

## RAG 检索参数

```yaml
rag:
  advisor:
    top-k: ${RAG_TOP_K:5}                      # 召回片段数
    similarity-threshold: ${RAG_SIMILARITY_THRESHOLD:0.5}  # 相似度阈值
```

检索由 `QuestionAnswerAdvisor` 完成，Advisor 链顺序为
**聊天记忆 → RAG 检索 → 调用模型**：记忆 advisor 会先写入原始用户问题，
因此增强过的 prompt 不会污染聊天记录。

## 文档入库流程

上传接口只负责把文件存进 MinIO 并登记 `file_record`，解析与向量化在
`ragTaskExecutor` 线程池中异步完成，过程中通过 SSE 推送 `doc-status` 事件：

```
parsing(10%) → parsing(40%) → parsing(70%) → ready(100%)
```

失败时状态置为 `failed` 并携带 `message`，前端展示失败原因。
