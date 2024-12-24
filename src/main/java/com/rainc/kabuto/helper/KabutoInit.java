package com.rainc.kabuto.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import java.sql.PreparedStatement;
import java.util.HashMap;

/**
 * @author rainc
 * @date 2023/4/11
 */
@Slf4j
public class KabutoInit implements CommandLineRunner {
    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    static String INIT_ASYNC_TASK_TABLE ="CREATE TABLE `kabuto_async_task` (\n" +
            "  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id主键',\n" +
            "  `retry_count` bigint(20) DEFAULT NULL COMMENT '重试次数',\n" +
            "  `interval` bigint(20) DEFAULT NULL COMMENT '间隔时间',\n" +
            "  `interval_unit` varchar(255) DEFAULT NULL COMMENT '间隔时间单位',\n" +
            "  `cron` varchar(255) DEFAULT NULL COMMENT 'cron表达式',\n" +
            "  `task_type` varchar(255) DEFAULT NULL COMMENT '任务类型',\n" +
            "  `business_type` varchar(255) DEFAULT NULL COMMENT '业务类型',\n" +
            "  `business_id` varchar(255) DEFAULT NULL COMMENT '业务id',\n" +
            "  `task_param` text COMMENT '任务参数',\n" +
            "  `trigger_last_time` bigint(20) DEFAULT NULL COMMENT '最后一次调度时间',\n" +
            "  `trigger_next_time` bigint(20) DEFAULT NULL COMMENT '下次调度时间',\n" +
            "  `status` varchar(255) DEFAULT NULL COMMENT '状态',\n" +
            "  `last_ip` varchar(255) DEFAULT NULL COMMENT '最后运行ip',\n" +
            "  `update_time` datetime(6) DEFAULT NULL COMMENT '更新时间',\n" +
            "  `create_time` datetime(6) DEFAULT NULL COMMENT '创建时间',\n" +
            "  PRIMARY KEY (`id`),\n" +
            "  KEY `kabuto_async_task_status_trigger_next_time_last_ip_index` (`status`,`trigger_next_time`,`last_ip`)\n" +
            ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='异步任务表'";
    static String INIT_REGISTRY_TABLE ="CREATE TABLE IF NOT EXISTS kabuto_registry\n" +
            "(\n" +
            "\tip varchar(255) null comment 'ip地址',\n" +
            "\tactive tinyint(1) null comment '是否活跃',\n" +
            "\tupdate_time datetime null comment '心跳时间'\n" +
            ")\n" +
            "comment '注册表';\n" +
            "\n";
    @Override
    public void run(String... args) throws Exception {
        try {
            namedParameterJdbcTemplate.execute(INIT_ASYNC_TASK_TABLE, new HashMap<>(), PreparedStatement::execute);
            namedParameterJdbcTemplate.execute(INIT_REGISTRY_TABLE, new HashMap<>(), PreparedStatement::execute);
        } catch (DataAccessException e) {
            log.error("自动建表失败,如果没有建表，请尝试手动建表",e);
        }
    }

    public static void main(String[] args) {
        System.out.println(INIT_ASYNC_TASK_TABLE);
    }
}
