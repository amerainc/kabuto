package com.rainc.kabuto.enums;

/**
 * @author rainc
 * @date 2023/3/9
 */
public enum KabutoTaskStatusEnum {
    /**
     * 初始化
     */
    INIT,
    /**
     * 执行中
     */
    EXECUTING,
    /**
     * 成功
     */
    SUCCESS,
    /**
     * 失败
     */
    FAIL,
    /**
     * 取消
     */
    CANCEL,
    /**
     * 最终失败
     */
    FINALLY_FAIL;
}
