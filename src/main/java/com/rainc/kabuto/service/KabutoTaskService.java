package com.rainc.kabuto.service;

import com.rainc.kabuto.entity.KabutoTask;

/**
 * @author rainc
 * @date 2023/12/25
 */
public interface KabutoTaskService {
    /**
     * 提交
     * @param kabutoTask
     * @return
     */
    KabutoTask submit(KabutoTask kabutoTask);
}
