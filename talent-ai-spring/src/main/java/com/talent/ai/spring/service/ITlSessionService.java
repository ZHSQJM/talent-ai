package com.talent.ai.spring.service;


import com.baomidou.mybatisplus.spring.service.IService;
import com.talent.ai.spring.entity.FileRecord;
import com.talent.ai.spring.entity.TlSession;

/**
 * @author watson
 * @date 2026年/11月/06日/9:51
 */
public interface ITlSessionService extends IService<TlSession> {

    TlSession saveChat();
}
