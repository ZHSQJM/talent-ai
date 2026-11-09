package com.talent.ai.spring.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * @author watson
 * @date 2026年/11月/03日/11:16
 */
public interface StorageService {

    /**
     * 普通单文件上传
     * @param rename 是否重命名（true: 随机UUID前缀，false: 使用原文件名）
     */
    String upload(MultipartFile file, String path, boolean rename) throws Exception;


    /**
     * 预览
     * @param key
     * @param expire
     * @return
     */
    String presignedUrl(String key,  int expire);



    void delete(String key);

    /**
     * 文件下载
     */
    void download(String filePath, HttpServletResponse response) throws Exception;

    /**
     * 读取对象内容流（异步解析场景使用：请求结束后无法再读 MultipartFile，
     * 只能按 key 从对象存储取回文件内容）
     *
     * @param key 对象存储中的 key
     * @return 内容流，调用方负责关闭
     */
    java.io.InputStream load(String key) throws Exception;

    /**
     * 秒传检查：根据文件 MD5 检查文件是否存在
     * @return 如果存在返回文件访问路径，否则返回 null
     */
    String checkFileExists(String md5);

    /**
     * 初始化分片上传
     * @param md5 文件MD5
     * @param fileName 文件名
     * @param totalChunks 总分片数
     * @return 返回 uploadId（用于标识本次分片任务）
     */
    Map<String, Object> initMultipartUpload(String md5, String fileName, int totalChunks) throws Exception;

    /**
     * 上传分片
     */
    void uploadChunk(MultipartFile chunk, String uploadId, int partNumber, String md5) throws Exception;

    /**
     * 合并分片
     */
    String completeMultipartUpload(String uploadId, String md5, String fileName, int totalChunks,boolean rename) throws Exception;
}
