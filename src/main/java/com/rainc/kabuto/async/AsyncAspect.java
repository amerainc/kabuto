package com.rainc.kabuto.async;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.rainc.kabuto.entity.KabutoTask;
import com.rainc.kabuto.enums.KabutoTaskStatusEnum;
import com.rainc.kabuto.helper.KabutoContext;
import com.rainc.kabuto.repository.KabutoTaskRepository;
import com.rainc.kabuto.util.SpelUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.lang.reflect.Method;

/**
 * @author zhengyuchen
 * @date 2021/11/16
 */
@Slf4j
@Aspect
@Component
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class AsyncAspect {
    @Autowired
    KabutoTaskRepository kabutoTaskRepository;
    @Autowired
    KabutoContext kabutoContext;

    @Pointcut("@annotation(com.rainc.kabuto.async.Kabuto)")
    public void pointCut() {
    }

    @Around("pointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Object[] args = point.getArgs();
        String[] paramNames = ((CodeSignature) point.getSignature()).getParameterNames();
        JSONObject jsonObject = new JSONObject();
        for (int i = 0; i < args.length; i++) {
            jsonObject.put(paramNames[i], args[i]);
        }
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Kabuto kabuto = method.getAnnotation(Kabuto.class);
        KabutoTask kabutoTask = new KabutoTask();
        kabutoTask.setBusinessId(SpelUtil.analyseSpel(jsonObject, kabuto.businessId()));
        kabutoTask.setBusinessType(SpelUtil.analyseSpel(jsonObject, kabuto.businessType()));
        kabutoTask.setCron(kabuto.cron());
        kabutoTask.setTaskParam(jsonObject.toString());
        kabutoTask.setStatus(KabutoTaskStatusEnum.INIT);
        if (StrUtil.isEmpty(kabuto.taskType())) {
            kabutoTask.setTaskType(method.getName());
        } else {
            kabutoTask.setTaskType(kabuto.taskType());
        }
        kabutoTask.setInterval(kabuto.interval());
        kabutoTask.setIntervalUnit(kabuto.intervalUnit());
        kabutoTask.setLastIp(kabutoContext.IP);
        kabutoTask.setRetryCount(kabuto.retryCount());
        kabutoTaskRepository.insert(kabutoTask);
        return null;
    }
}
