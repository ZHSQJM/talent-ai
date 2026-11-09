package com.talent.ai.spring.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.talent.ai.spring.TalentResult;
import com.talent.ai.spring.bussiness.UploadService;
import com.talent.ai.spring.entity.FileRecord;
import com.talent.ai.spring.service.IFileRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author watson
 * @date 2026年/11月/06日/14:41
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileController {


    private final IFileRecordService fileRecordService;
    /**
     * 文件分页列表接口
     * @param page 当前页码（默认第1页）
     * @param pageSize 每页条数（默认10条）
     */
    @GetMapping("/page")
    public TalentResult<IPage<FileRecord>> getPageList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        // 创建 MyBatis-Plus 分页对象
        Page<FileRecord> pageParam = new Page<>(page, pageSize);

        // 执行分页查询
        IPage<FileRecord> pageResult = fileRecordService.page(pageParam);

        return TalentResult.success(pageResult);
    }
}
