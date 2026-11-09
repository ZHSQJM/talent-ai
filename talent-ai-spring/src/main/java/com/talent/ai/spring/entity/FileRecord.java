package com.talent.ai.spring.entity;

import lombok.Data;

/**
 * @author watson
 * @date 2026年/11月/06日/9:36
 */
@Data
public class FileRecord {

    private Long id;

    private String fileName;

    private String userId;

    /**
     * 文件大小，单位 KB
     */
    private Integer size;

    /**
     * 文件在对象存储（MinIO）中的 key，用于异步解析时回读文件内容
     */
    private String objectName;

    /**
     * 解析状态：parsing 解析中 / ready 就绪 / failed 失败
     */
    private String status;

    /**
     * 解析进度百分比 0-100
     */
    private Integer progress;

    /**
     * 切分后写入向量库的片段数
     */
    private Integer chunkCount;

    /**
     * 解析失败的原因
     */
    private String errorMsg;
}
