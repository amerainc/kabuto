package com.rainc.kabuto.handler;

import cn.hutool.core.util.StrUtil;
import com.rainc.kabuto.async.Kabuto;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rainc
 * @date 2023/4/11
 */
@Component
public class KabutoTaskHandlerFactory implements SmartInstantiationAwareBeanPostProcessor {
    /**
     * taskType 与处理类映射
     */
    private Map<String, KabutoTaskHandler> taskMap = new ConcurrentHashMap<>(28);
    public KabutoTaskHandler getTaskHandler(String type){
        return taskMap.get(type);
    }

    @Autowired(required = false)
    public void setTaskHandler(Map<String, KabutoTaskHandler> kabutoTaskHandlerMap) {
        kabutoTaskHandlerMap.forEach((k,v)->{
            Kabuto kabuto = AnnotatedElementUtils.findMergedAnnotation(v.getClass(), Kabuto.class);
            String taskType=k;
            if (kabuto!=null&& StrUtil.isNotEmpty(kabuto.taskType())){
               taskType=kabuto.taskType();
            }
            KabutoTaskHandler task = kabutoTaskHandlerMap.putIfAbsent(taskType, v);
            if (task != null) {
                throw new IllegalStateException("taskType在如下两个函数中重复定义: " + taskType);
            }
        });
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        ReflectionUtils.doWithMethods(bean.getClass(), method -> {
            //注册带有Async注解的函数
            Kabuto async = AnnotatedElementUtils.findMergedAnnotation(method, Kabuto.class);
            if (async == null) {
                return;
            }
            Method onSuccess = ReflectionUtils.findMethod(bean.getClass(), async.onSuccess(), method.getParameterTypes());
            Method onFinallyFail = ReflectionUtils.findMethod(bean.getClass(), async.onFinallyFail(), method.getParameterTypes());
            KabutoMethodTaskHandler kabutoMethodTaskHandler = new KabutoMethodTaskHandler(method, onSuccess, onFinallyFail, bean);
            String taskType = async.taskType();
            if (StrUtil.isEmpty(taskType)){
                taskType=method.getName();
            }
            KabutoTaskHandler task = taskMap.putIfAbsent(taskType, kabutoMethodTaskHandler);
            if (task != null) {
                throw new IllegalStateException("taskType在如下两个函数中重复定义: " + taskType);
            }
        });
        return true;
    }
}
