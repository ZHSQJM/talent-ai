package com.talent.ai.spring.bussiness;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.talent.ai.spring.dto.DocStatusEvent;
import com.talent.ai.spring.entity.FileRecord;
import com.talent.ai.spring.service.IFileRecordService;
import com.talent.ai.spring.service.SseService;
import com.talent.ai.spring.service.StorageService;
import com.talent.ai.spring.utils.DocumentParseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 文档入库（解析 -> 切分 -> 向量化）服务。
 * <p>
 * 解析文档（尤其是 PDF / Word）是重 IO 的操作，放在上传请求里同步执行会让接口超时，
 * 因此这里用 {@link Async} 放到独立线程池里跑：
 * <ul>
 *   <li>上传接口只负责把文件存到 MinIO 并落一条 file_record 记录（status=parsing），立即返回；</li>
 *   <li>后台线程解析文件、切分、向量化，每完成一个阶段就更新数据库状态，
 *       并通过 SSE 把 {@code doc-status} 事件推给前端，前端实时刷新进度条。</li>
 * </ul>
 *
 * @author watson
 * @date 2026年/11月/06日/18:30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestService {

    /**
     * 解析中
     */
    public static final String STATUS_PARSING = "parsing";

    /**
     * 已就绪（可被检索）
     */
    public static final String STATUS_READY = "ready";

    /**
     * 解析失败
     */
    public static final String STATUS_FAILED = "failed";

    private final StorageService storageService;

    private final IFileRecordService fileRecordService;

    private final DocumentParseUtil documentParseUtil;

    private final TokenTextSplitter tokenTextSplitter;

    private final VectorStore vectorStore;

    private final SseService sseService;

    /**
     * 执行文档入库。
     * <p>
     * 注意：本方法必须是 {@code public} 且由<b>其它 Bean</b> 调用才会走异步代理，
     * 同一个类内部自调用不会生效（Spring AOP 自调用失效）。
     *
     * @param fileId     file_record 主键
     * @param objectName MinIO 中的对象 key
     * @param fileName   原始文件名（用于确定解析器、写入向量元数据）
     * @param userId     所属用户，SSE 推送的接收方
     */
    @Async("ragTaskExecutor")
    public void ingest(Long fileId, String objectName, String fileName, String userId) {
        Path tempFile = null;
        try {
            // 1. 从对象存储取回文件（上传请求已结束，MultipartFile 不能再读）
            tempFile = downloadToTempFile(objectName, fileName);
            updateStatus(fileId, fileName, STATUS_PARSING, 10, null, null, userId);

            // 2. 解析文本
            Resource resource = new FileSystemResource(tempFile);
            List<Document> documents = documentParseUtil.parse(resource, UploadService.getFileSuffix(fileName));
            if (documents.isEmpty() || documents.stream().allMatch(d -> d.getText() == null || d.getText().isBlank())) {
                throw new IllegalStateException("文档中没有解析到任何文本内容");
            }
            updateStatus(fileId, fileName, STATUS_PARSING, 40, null, null, userId);

            // 3. 切分
            List<Document> chunks = tokenTextSplitter.apply(documents);
            if (chunks.isEmpty()) {
                throw new IllegalStateException("文档切分后没有产生任何片段");
            }
            for (Document chunk : chunks) {
                chunk.getMetadata().put("fileName", fileName);
                chunk.getMetadata().put("fileId", fileId);
            }
            updateStatus(fileId, fileName, STATUS_PARSING, 70, null, null, userId);

            // 4. 写入向量库（先清掉该文件此前的向量，避免重复上传造成重复召回）
            deleteOldVectors(fileId);
            vectorStore.add(chunks);

            // 5. 完成
            updateStatus(fileId, fileName, STATUS_READY, 100, chunks.size(), null, userId);
            log.info("[ingest] 文档入库完成, fileId={}, fileName={}, chunks={}", fileId, fileName, chunks.size());
        } catch (Exception e) {
            log.error("[ingest] 文档入库失败, fileId={}, fileName={}", fileId, fileName, e);
            updateStatus(fileId, fileName, STATUS_FAILED, 100, null, e.getMessage(), userId);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                    // 临时文件删除失败不影响主流程
                }
            }
        }
    }

    /**
     * 把对象存储中的文件下载到本地临时文件。
     * <p>
     * 用临时文件而不是内存流，是因为 Tika 需要真实文件名来识别文档类型，
     * FileSystemResource 能提供 getFilename()，InputStreamResource 不能。
     */
    private Path downloadToTempFile(String objectName, String fileName) throws Exception {
        String suffix = UploadService.getFileSuffix(fileName);
        Path tempFile = Files.createTempFile("rag-ingest-", suffix.isEmpty() ? ".tmp" : "." + suffix);
        try (InputStream in = storageService.load(objectName)) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempFile;
    }

    /**
     * 清理同一文件此前写入的向量片段。
     * <p>
     * 失败不阻断流程：最坏情况是同一文件重复上传会召回到两份内容。
     */
    private void deleteOldVectors(Long fileId) {
        try {
            vectorStore.delete("fileId == " + fileId);
        } catch (Exception e) {
            log.warn("[ingest] 清理旧向量失败（忽略）, fileId={}: {}", fileId, e.getMessage());
        }
    }

    /**
     * 更新解析状态并推送 SSE 事件。
     */
    private void updateStatus(Long fileId, String fileName, String status, Integer progress,
                              Integer chunkCount, String errorMsg, String userId) {
        try {
            LambdaUpdateWrapper<FileRecord> wrapper = new LambdaUpdateWrapper<FileRecord>()
                    .eq(FileRecord::getId, fileId)
                    .set(FileRecord::getStatus, status)
                    .set(FileRecord::getProgress, progress)
                    .set(FileRecord::getErrorMsg, errorMsg);
            if (chunkCount != null) {
                wrapper.set(FileRecord::getChunkCount, chunkCount);
            }
            fileRecordService.update(wrapper);
        } catch (Exception e) {
            log.error("[ingest] 更新文件状态失败, fileId={}", fileId, e);
        }

        try {
            sseService.sendDocStatus(userId, DocStatusEvent.of(fileId, fileName, status, progress, chunkCount, errorMsg));
        } catch (Exception e) {
            // SSE 推送失败不影响入库流程
            log.warn("[ingest] 推送 doc-status 事件失败, fileId={}: {}", fileId, e.getMessage());
        }
    }
}
