package com.talent.ai.spring.controller.test;



import com.talent.ai.spring.TalentResult;
import com.talent.ai.spring.service.StorageService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/file/storage")
public class StorageController {

    @Autowired
    private StorageService storageService;

    /**
     * 0. 普通单文件上传（支持重命名控制）
     * @param rename 默认true（重命名），传false则使用原文件名
     */
    @PostMapping("/upload")
    public TalentResult<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "path", defaultValue = "auto") String path,
            @RequestParam(value = "rename", defaultValue = "true") boolean rename) throws Exception {
        String filePath = storageService.upload(file, path, rename);
        return TalentResult.success(filePath);
    }

    @GetMapping("/preView")
    public TalentResult<String> preView(
            @RequestParam("key") String key,
            @RequestParam(value = "ttl",defaultValue = "5") int ttl) throws Exception {
        String presignedUrl = storageService.presignedUrl(key, ttl);
        return TalentResult.success(presignedUrl);
    }
    @GetMapping("/delete")
    public TalentResult<Void> preView(
            @RequestParam("key") String key
          ) throws Exception {
         storageService.delete(key);
        return TalentResult.success();
    }


    /**
     * 1. 秒传检查接口
     */
    @GetMapping("/check")
    public ResponseEntity checkFile(@RequestParam("md5") String md5) {
        String existingPath = storageService.checkFileExists(md5);
        HashMap<String,Object> map = new HashMap<>();
        if (existingPath != null) {
            map.put("path", existingPath);
            map.put("exists", true);
            return ResponseEntity.ok(map);
        }
        map.put("path", false);
        return ResponseEntity.ok(map);
    }

    /**
     * 2. 初始化分片上传
     */
    @PostMapping("/init")
    public ResponseEntity initUpload(
            @RequestParam("md5") String md5,
            @RequestParam("fileName") String fileName,
            @RequestParam("totalChunks") int totalChunks) throws Exception {
        Map<String, Object> result = storageService.initMultipartUpload(md5, fileName, totalChunks);
        return ResponseEntity.ok(result);
    }

    /**
     * 3. 上传单个分片
     */
    @PostMapping("/upload-chunk")
    public ResponseEntity<String> uploadChunk(
            @RequestParam("chunk") MultipartFile chunk,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("partNumber") int partNumber,
            @RequestParam("md5") String md5) throws Exception {
        storageService.uploadChunk(chunk, uploadId, partNumber, md5);
        return ResponseEntity.ok("分片上传成功");
    }

    /**
     * 4. 合并分片（支持重命名控制）
     * @param rename 默认true（重命名），传false则使用原文件名
     */
    @PostMapping("/complete")
    public ResponseEntity<String> completeUpload(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("md5") String md5,
            @RequestParam("fileName") String fileName,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam(value = "rename", defaultValue = "true") boolean rename) throws Exception {
        String filePath = storageService.completeMultipartUpload(uploadId, md5, fileName, totalChunks, rename);
        return ResponseEntity.ok("上传完成，文件路径: " + filePath);
    }

    /**
     * 5. 普通下载接口
     */
    @GetMapping("/download")
    public void download(@RequestParam("filePath") String filePath, HttpServletResponse response) throws Exception {
        storageService.download(filePath, response);
    }
}

