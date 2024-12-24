package com.rainc.kabuto.async;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * @author rainc
 * @date 2023/2/3
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Kabuto {
    /**
     * 重试次数
     * @return
     */
    int retryCount() default 1;
    /**
     * 业务类型支持spel表达式
     * @return
     */
    String businessType() default "#businessType";
    /**
     * 业务id支持spel表达式
     * @return
     */
    String businessId() default "#businessId";
    /**
     * 任务类型支持spel表达式，默认使用函数名
     * @return
     */
    String taskType() default "";
    /**
     * 间隔时间单位
     * @return
     */
    TimeUnit intervalUnit() default TimeUnit.SECONDS;
    /**
     * 间隔时间
     * @return
     */
    long interval() default 60;
    /**
     * cron表达式
     * @return
     */
    String cron() default "";
    /**
     * 成功时执行的函数，同类下的函数名（入参要与主函数一致）
     * @return
     */
    String onSuccess() default "";

    /**
     * 最终失败执行的函数，同类下的函数名（入参要与主函数一致）
     * @return
     */
    String onFinallyFail() default "";
}
