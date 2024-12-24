package com.rainc.kabuto.helper;

import com.rainc.kabuto.entity.KabutoTask;
import com.rainc.kabuto.enums.KabutoTaskStatusEnum;
import com.rainc.kabuto.handler.KabutoTaskHandler;
import com.rainc.kabuto.handler.KabutoTaskHandlerFactory;
import com.rainc.kabuto.repository.KabutoTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 异步任务runnable
 *
 * @author zhengyuchen
 * @date 2021/9/16
 */
@Slf4j
@Component
@Scope("prototype")
public class KabutoTaskRunnable implements Runnable {
    @Autowired
    KabutoTaskRepository kabutoTaskRepository;
    @Autowired
    KabutoTaskHandlerFactory factory;
    @Autowired
    KabutoContext kabutoContext;
    private KabutoTask kabutoTask;

    public void setKabutoTask(KabutoTask kabutoTask) {
        this.kabutoTask = kabutoTask;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        log.info("任务处理中，taskId{}", kabutoTask.getId());
        KabutoTaskHandler taskHandler = factory.getTaskHandler(kabutoTask.getTaskType());
        KabutoTaskStatusEnum status;
        try {
            status = taskHandler.run(kabutoTask);
        } catch (Exception e) {
            log.error("任务处理异常，taskId{}", kabutoTask.getId(),e);
            status=KabutoTaskStatusEnum.FAIL;
        }
        //更新耗时
        kabutoContext.updateCostTime(kabutoTask,System.currentTimeMillis()-start);
        kabutoTask.setStatus(status);
        switch (status){
            case SUCCESS:
                try {
                    taskHandler.onSuccess(kabutoTask);
                } catch (Exception ignored) {
                }
                break;
            case FAIL:
                Integer retryCount = kabutoTask.getRetryCount();
                kabutoTask.setRetryCount(--retryCount);
                if (retryCount<=0){
                    kabutoTask.setStatus(KabutoTaskStatusEnum.FINALLY_FAIL);
                    try {
                        taskHandler.onFinallyFail(kabutoTask);
                    } catch (Exception ignored) {
                    }
                }else {
                    kabutoTask.setStatus(KabutoTaskStatusEnum.FAIL);
                }
        }

        kabutoTaskRepository.update(kabutoTask);
        kabutoContext.removeRunningConfig(kabutoTask.getId());
    }

    /**
     * 拒绝策略
     */
    public void rejected() {
        kabutoTask.setStatus(KabutoTaskStatusEnum.FAIL);
        //直接置为失败
        kabutoTaskRepository.update(kabutoTask);
        log.warn("异步任务池已满，taskId{}执行失败", kabutoTask.getId());

    }
}
