package com.talent.ai.spring.dto;

import lombok.Data;

/**
 * 通过 SSE 推送给前端的文档解析状态事件。
 * <p>
 * 序列化后作为 SSE 帧的 data 部分，例如：
 * <pre>
 * data:{"type":"doc-status","fileId":12,"fileName":"a.docx","status":"parsing","progress":40,"chunkCount":0,"message":null}
 * </pre>
 * 前端按 fileId 就地更新对应文档的解析状态与进度。
 *
 * @author watson
 * @date 2026年/11月/06日/18:20
 */
@Data
public class DocStatusEvent {

    /**
     * 事件类型，固定为 doc-status
     */
    private String type;

    /**
     * 文件记录 ID，前端据此定位列表中的条目
     */
    private Long fileId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 解析状态：parsing / ready / failed
     */
    private String status;

    /**
     * 解析进度百分比 0-100
     */
    private Integer progress;

    /**
     * 切分后的向量片段数（就绪后才有值）
     */
    private Integer chunkCount;

    /**
     * 补充信息，失败时为错误原因
     */
    private String message;

    public static DocStatusEvent of(Long fileId, String fileName, String status, Integer progress,
                                    Integer chunkCount, String message) {
        DocStatusEvent event = new DocStatusEvent();
        event.setType("doc-status");
        event.setFileId(fileId);
        event.setFileName(fileName);
        event.setStatus(status);
        event.setProgress(progress);
        event.setChunkCount(chunkCount);
        event.setMessage(message);
        return event;
    }
}
