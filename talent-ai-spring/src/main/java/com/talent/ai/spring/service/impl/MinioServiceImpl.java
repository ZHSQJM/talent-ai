package com.talent.ai.spring.service.impl;

import com.talent.ai.spring.service.StorageService;
import io.minio.*;
import io.minio.http.Method;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.talent.ai.spring.config.MinioProperties;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


/**
 * @author watson
 * @date 2026年/11月/04日/15:15
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "minio", matchIfMissing = true)
@RequiredArgsConstructor
public class MinioServiceImpl implements StorageService {

    private final MinioProperties minioProperties;

    private final MinioClient minioClient;

    // 模拟数据库/Redis 存储：MD5 -> 文件访问路径
    private static final Map<String, String> MD5_CACHE = new ConcurrentHashMap<>();
    // 模拟分片任务缓存：uploadId -> 附加信息
    private static final Map<String, String> UPLOAD_ID_CACHE = new ConcurrentHashMap<>();


    @Override
    public String upload(MultipartFile file, String path, boolean rename) throws Exception {
        String originalFilename = file.getOriginalFilename();
        if(StringUtils.isBlank(path)){
            path = "customer";
        }
        String objectKey = newObjectKey(originalFilename, path, rename);
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }
        return objectKey;
    }

    @Override
    public String presignedUrl(String key, int expiry) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucketName())
                            .object(key)
                            .expiry(expiry, TimeUnit.MINUTES)
                    .build());
        }catch (Exception e){
            throw new IllegalStateException("签名失败，获取访问连接失败"+key,e);
        }
    }

    @Override
    public void delete(String key) {

        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                    .object(key)
                    .build());
        }catch (Exception e){
            throw new IllegalStateException("删除失败"+key,e);
        }
    }

    @Override
    public void download(String filePath, HttpServletResponse response) throws Exception {

        response.setContentType("application/octet-stream");
        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder().bucket(minioProperties.getBucketName()).object(filePath).build()
        );

        org.apache.commons.io.IOUtils.copy(stream, response.getOutputStream());
        stream.close();
    }

    @Override
    public InputStream load(String key) throws Exception {
        // GetObjectResponse 实现了 InputStream 与 Closeable，调用方必须关闭
        return minioClient.getObject(
                GetObjectArgs.builder().bucket(minioProperties.getBucketName()).object(key).build()
        );
    }

    @Override
    public String checkFileExists(String md5) {
        return MD5_CACHE.get(md5);
    }

    @Override
    public Map<String, Object> initMultipartUpload(String md5, String fileName, int totalChunks) throws Exception {
        String uploadId = UUID.randomUUID().toString();
        UPLOAD_ID_CACHE.put(uploadId, md5);

        Map<String, Object> result = new HashMap<>();
        result.put("uploadId", uploadId);
        return result;
    }

    @Override
    public void uploadChunk(MultipartFile chunk, String uploadId, int partNumber, String md5) throws Exception {
        String chunkObjectName = "temp/" + uploadId + "/part-" + partNumber;
        try (InputStream inputStream = chunk.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(chunkObjectName)
                            .stream(inputStream, chunk.getSize(), -1)
                            .build()
            );
        }
    }

    @Override
    public String completeMultipartUpload(String uploadId, String md5, String fileName, int totalChunks,boolean rename) throws Exception {
        // 根据 rename 参数决定合并后的最终文件名
        String finalObjectName = "files/" + (rename ? (UUID.randomUUID() + "_" + fileName) : fileName);
        List<ComposeSource> sources = new ArrayList<>();

        for (int i = 1; i <= totalChunks; i++) {
            sources.add(
                    ComposeSource.builder()
                            .bucket(minioProperties.getBucketName())
                            .object("temp/" + uploadId + "/part-" + i)
                            .build()
            );
        }

        minioClient.composeObject(
                ComposeObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(finalObjectName)
                        .sources(sources)
                        .build()
        );

        MD5_CACHE.put(md5, finalObjectName);
        UPLOAD_ID_CACHE.remove(uploadId);

        return finalObjectName;
    }

    private String newObjectKey(String originalFilename,String path,boolean rename ){
        String datePath = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        String cleanPath = path.endsWith("/") ? path : path + "/";
        // 根据 rename 参数决定文件名

        String fileName = rename ? (UUID.randomUUID() + "_" + originalFilename) : originalFilename;
        return datePath + "/" +cleanPath+ fileName;
    }
}
