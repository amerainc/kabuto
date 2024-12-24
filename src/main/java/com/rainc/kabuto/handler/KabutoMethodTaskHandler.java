package com.rainc.kabuto.handler;

import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson.JSONObject;
import com.rainc.kabuto.entity.KabutoTask;
import com.rainc.kabuto.async.Kabuto;
import com.rainc.kabuto.enums.KabutoTaskStatusEnum;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;

/**
 * @author rainc
 * @date 2023/4/11
 */
@Kabuto
public class KabutoMethodTaskHandler implements KabutoTaskHandler{
    /**
     * 调用方法
     */
    private final Method run;
    /**
     * 成功时调用的方法
     */
    private final Method onSuccess;
    /**
     * 最终失败时调用的方法
     */
    private final Method onFinallyFail;
    /**
     * 调用实例
     */
    private final Object target;

    public KabutoMethodTaskHandler(Method run, Method onSuccess, Method onFinallyFail, Object target) {
        this.run = run;
        this.onSuccess = onSuccess;
        this.onFinallyFail = onFinallyFail;
        this.target = target;
    }

    @Override
    public KabutoTaskStatusEnum run(KabutoTask task) throws Exception {
        Object invoke = doMethod(task, run);
        if (invoke instanceof KabutoTaskStatusEnum) {
            return (KabutoTaskStatusEnum) invoke;
        }
        return KabutoTaskStatusEnum.SUCCESS;
    }
    @Override
    public void onSuccess(KabutoTask task) {
        doMethod(task, onSuccess);
    }

    @Override
    public void onFinallyFail(KabutoTask task) {
        doMethod(task, onFinallyFail);
    }

    private Object doMethod(KabutoTask task, Method method) {
        if (method==null){
            return null;
        }
        int parameterCount = method.getParameterCount();
        if (parameterCount==0){
            return ReflectUtil.invoke(target, method);
        }
        ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
        JSONObject jsonObject = JSONObject.parseObject(task.getTaskParam());
        Object[] args=new Object[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            MethodParameter methodParameter = MethodParameter.forExecutable(method, i);
            methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
            String argJson = JSONObject.toJSONString(jsonObject.get(methodParameter.getParameterName()));
            args[i]=JSONObject.parseObject(argJson,methodParameter.getGenericParameterType());
        }
        //如果只有一个参数，并且解析后为空，尝试直接反序列化该参数
        if (args.length==1&&args[0]==null){
            args[0]=JSONObject.parseObject(task.getTaskParam(),method.getParameterTypes()[0]);
        }
        return ReflectUtil.invoke(target, method, args);
    }
}
