package com.talent.ai.spring.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;

import com.talent.ai.spring.entity.TlSession;
import com.talent.ai.spring.mapper.TlSessionMapper;
import com.talent.ai.spring.service.ITlSessionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author watson
 * @date 2026年/11月/06日/9:53
 */
@Service
public class TlSessionServiceImpl extends ServiceImpl<TlSessionMapper, TlSession> implements ITlSessionService {
    @Override
    public TlSession saveChat() {
        TlSession session = new TlSession();
        session.setCreateTime(LocalDateTime.now());
        session.setUserId("1");
        save(session);
        return session;
    }
}
