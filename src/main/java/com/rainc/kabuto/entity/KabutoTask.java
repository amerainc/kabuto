package com.rainc.kabuto.entity;

import com.rainc.kabuto.enums.KabutoTaskStatusEnum;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author rainc
 * @date 2023/2/6
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KabutoTask {
    /**
     * id主键
     */
    Long id;
    /**
     * 重试次数
     */
    Integer retryCount;
    /**
     * 间隔时间
     */
    Long interval;
    /**
     * 间隔时间单位
     */
    TimeUnit intervalUnit;
    /**
     * cron表达式
     */
    String cron;
    /**
     * 任务类型
     */
    String taskType;
    /**
     * 业务类型
     */
    String businessType;
    /**
     * 业务id
     */
    String businessId;
    /**
     * 任务参数
     */
    String taskParam;
    /**
     * 创建时间
     */
    Date createTime;
    /**
     * 修改时间
     */
    Date updateTime;
    /**
     * 状态
     */
    KabutoTaskStatusEnum status;
    /**
     * 最后一次调度时间
     */
    Long triggerLastTime;

    /**
     * 下次调度时间
     */
    Long triggerNextTime;
    /**
     * 最后执行的ip
     */
    String lastIp;
}
