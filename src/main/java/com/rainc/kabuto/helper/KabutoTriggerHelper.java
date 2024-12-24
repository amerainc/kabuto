package com.rainc.kabuto.helper;

import cn.hutool.extra.spring.SpringUtil;
import com.rainc.kabuto.entity.KabutoTask;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * @author rainc
 * @date 2023/3/9
 */
@Component
@Slf4j
public class KabutoTriggerHelper  implements ApplicationContextAware {
    @Resource
    private Executor kabutoTaskExecutor;
    @Resource
    private Executor kabutoTaskSlowExecutor;
    private ApplicationContext applicationContext;
    @Autowired
    KabutoContext kabutoContext;

    public void trigger(KabutoTask kabutoTask) {
        KabutoTaskRunnable kabutoTaskRunnable = applicationContext.getBean(KabutoTaskRunnable.class);
        kabutoContext.addRunningConfig(kabutoTask.getId());
        kabutoTaskRunnable.setKabutoTask(kabutoTask);
        //选择执行线程
        Executor executor=kabutoTaskExecutor;
        if (kabutoTaskSlowExecutor!=null){
            executor=kabutoContext.isSlow(kabutoTask)?kabutoTaskSlowExecutor:kabutoTaskExecutor;
        }
        executor.execute(kabutoTaskRunnable);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext=applicationContext;
    }
}
