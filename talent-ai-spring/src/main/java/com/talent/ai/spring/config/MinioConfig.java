package com.talent.ai.spring.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName：MinioConfig
 * @Author: hszhou
 * @Date: 2025/1/7 13:21
 * @Description: 必须描述类做什么事情, 实现什么功能
 */


@RequiredArgsConstructor
@Configuration
public class MinioConfig {

    private final  MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient(){

        MinioClient minioClient = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();


        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .build());
            if(!exists){
                minioClient.makeBucket(MakeBucketArgs.builder()
                                .bucket(minioProperties.getBucketName())
                        .build());
            }

        }catch (Exception e){
            throw new IllegalStateException("minio 初始化失败："+minioProperties.getEndpoint(),e);
        }
        return minioClient;
    }
}
