package com.talent.ai.spring.service.impl;


import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.talent.ai.spring.entity.FileRecord;
import com.talent.ai.spring.entity.TlSession;
import com.talent.ai.spring.mapper.FileRecordMapper;
import com.talent.ai.spring.service.IFileRecordService;
import org.springframework.stereotype.Service;

/**
 * @author watson
 * @date 2026年/11月/06日/9:52
 */
@Service
public class FileRecordServiceImpl extends ServiceImpl<FileRecordMapper, FileRecord> implements IFileRecordService {
}
