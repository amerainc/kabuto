package com.rainc.kabuto.entity;

import lombok.Data;

import java.util.Date;

/**
 * 注册信息
 * @author rainc
 * @date 2023/4/10
 */
@Data
public class Registry {
    /**
     * 平台ip
     */
    private String ip;

    /**
     * 活动状态
     */
    private Boolean active;

    /**
     * 更新时间
     */
    private Date updateTime;
}
