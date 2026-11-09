package com.talent.ai.spring.controller;

import com.talent.ai.spring.TalentResult;
import com.talent.ai.spring.bussiness.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author watson
 * @date 2026年/11月/06日/14:41
 */

@RestController
@RequiredArgsConstructor
public class UploadController {


    private final UploadService uploadService;

    /**
     * 上传文档。
     * <p>
     * 接口只负责存储与登记，解析/向量化在后台异步执行，
     * 进度通过 SSE 的 doc-status 事件推送。
     *
     * @param file   文档文件
     * @param userId 所属用户（可选，默认 "1"），决定 SSE 事件推给哪条连接
     * @return 对象存储中的 key
     */
    @PostMapping("/upload")
    public TalentResult<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) String userId) throws Exception {
        String upload = uploadService.upload(file, userId);
        return TalentResult.success(upload);
    }
}
