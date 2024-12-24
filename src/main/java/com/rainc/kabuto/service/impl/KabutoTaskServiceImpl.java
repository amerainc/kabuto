package com.rainc.kabuto.service.impl;

import com.rainc.kabuto.entity.KabutoTask;
import com.rainc.kabuto.repository.KabutoTaskRepository;
import com.rainc.kabuto.service.KabutoTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author rainc
 * @date 2023/12/25
 */
@Service
public class KabutoTaskServiceImpl implements KabutoTaskService {
    @Autowired
    KabutoTaskRepository kabutoTaskRepository;
    @Override
    public KabutoTask submit(KabutoTask kabutoTask) {
        kabutoTaskRepository.insert(kabutoTask);
        return kabutoTask;
    }
}
