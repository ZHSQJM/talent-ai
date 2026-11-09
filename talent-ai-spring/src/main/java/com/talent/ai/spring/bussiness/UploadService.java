package com.talent.ai.spring.bussiness;

import com.talent.ai.spring.entity.FileRecord;
import com.talent.ai.spring.service.IFileRecordService;
import com.talent.ai.spring.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务。
 * <p>
 * 上传只做两件事：把文件存到对象存储、落一条 file_record 记录（status=parsing），
 * 然后把「解析 -> 切分 -> 向量化」交给 {@link DocumentIngestService} 在后台线程执行，
 * 避免大文件解析把上传接口拖到超时。解析进度通过 SSE 的 doc-status 事件实时通知前端。
 *
 * @author watson
 * @date 2026年/11月/06日/9:56
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    /**
     * 默认用户 ID，与 SseService.DEFAULT_USER_ID 保持一致
     */
    private static final String DEFAULT_USER_ID = "1";

    private final StorageService storageService;

    private final IFileRecordService fileRecordService;

    private final DocumentIngestService documentIngestService;

    /**
     * 保存文件并触发异步入库
     *
     * @param file    上传的文件
     * @param userId  所属用户，缺省为 DEFAULT_USER_ID
     * @return 对象存储中的 key
     */
    public String upload(MultipartFile file, String userId) throws Exception {
        String upload = storageService.upload(file, "/rag", false);
        long size = file.getSize();
        String owner = (userId == null || userId.isBlank()) ? DEFAULT_USER_ID : userId;

        FileRecord fileRecord = new FileRecord();
        fileRecord.setFileName(file.getOriginalFilename());
        fileRecord.setUserId(owner);
        // 后端统一按 KB 存，前端 formatSize 也按 KB 展示
        fileRecord.setSize((int) size / 1024);
        fileRecord.setObjectName(upload);
        fileRecord.setStatus(DocumentIngestService.STATUS_PARSING);
        fileRecord.setProgress(0);
        fileRecord.setChunkCount(0);
        fileRecordService.save(fileRecord);

        // 异步解析入库：解析在后台线程进行，进度通过 SSE 推送
        documentIngestService.ingest(fileRecord.getId(), upload, file.getOriginalFilename(), owner);

        return upload;
    }

    /**
     * 获取文件后缀，不带点
     *
     * @param fileName 文件名
     * @return 小写后缀，没有后缀返回空字符串
     */
    public static String getFileSuffix(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") <= 0) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
