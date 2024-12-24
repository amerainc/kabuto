package com.rainc.kabuto.repository;

import com.rainc.kabuto.entity.KabutoTask;

import java.util.List;

/**
 * @author rainc
 * @date 2023/3/3
 */
public interface KabutoTaskRepository {
    /**
     * 预读异步任务
     * @param nowTime 当前时间
     * @return
     */
    List<KabutoTask> preloadAsyncTask(long nowTime);

    /**
     * 新增任务
     * @param kabutoTask
     */
    void insert(KabutoTask kabutoTask);

    /**
     * 更新任务
     * @param kabutoTask
     */
    void update(KabutoTask kabutoTask);

    /**
     * cas更新
     * @param kabutoTask
     * @param oldTriggerNextTime
     * @return
     */

    int casUpdate(KabutoTask kabutoTask, Long oldTriggerNextTime);
}
