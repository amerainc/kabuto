package com.rainc.kabuto.repository.impl;

import com.rainc.kabuto.config.KabutoAutoConfig;
import com.rainc.kabuto.entity.KabutoTask;
import com.rainc.kabuto.helper.KabutoContext;
import com.rainc.kabuto.repository.KabutoTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rainc
 * @date 2023/3/3
 */
@Component
public class NamedParameterJdbcTemplateKabutoTaskRepository implements KabutoTaskRepository {
    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    KabutoAutoConfig.KabutoProperties kabutoProperties;
    @Autowired
    KabutoContext kabutoContext;
    final String PRELOAD_ASYNC_TASK_SQL = "select * from kabuto_async_task where status in ('INIT','EXECUTING','FAIL') and trigger_next_time<:now order by trigger_next_time asc";
    final String PRELOAD_ASYNC_TEST_TASK_SQL = "select * from kabuto_async_task where status in ('INIT','EXECUTING','FAIL') and trigger_next_time<:now and last_ip=:ip order by trigger_next_time asc";

    @Override
    public List<KabutoTask> preloadAsyncTask(long nowTime) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("now", nowTime);
        params.put("ip", kabutoContext.IP);
        return namedParameterJdbcTemplate.query(kabutoProperties.isTest()?PRELOAD_ASYNC_TEST_TASK_SQL:PRELOAD_ASYNC_TASK_SQL, params, new BeanPropertyRowMapper<>(KabutoTask.class));
    }

    final String INSERT_SQL = "INSERT\n" +
            "INTO kabuto_async_task (retry_count, `interval`, interval_unit, task_type, business_type, business_id,\n" +
            "                                  task_param, trigger_last_time, trigger_next_time, status, last_ip,\n" +
            "                                  create_time)\n" +
            "VALUES (:retryCount, :interval, :intervalUnit, :taskType, :businessType, :businessId, :taskParam, 0,\n" +
            "        0, :status, :lastIp,\n" +
            "        now());";

    @Override
    public void insert(KabutoTask kabutoTask) {
        BeanPropertySqlParameterSource bs = new BeanPropertySqlParameterSource(kabutoTask);
        bs.registerSqlType("intervalUnit", Types.VARCHAR);
        bs.registerSqlType("status", Types.VARCHAR);
        namedParameterJdbcTemplate.update(INSERT_SQL, bs);
    }

    final String UPDATE_SQL = "UPDATE kabuto_async_task\n" +
            "SET retry_count       = :retryCount,\n" +
            "    trigger_next_time = :triggerNextTime,\n" +
            "    status            = :status,\n" +
            "    last_ip           = :lastIp,\n" +
            "    update_time       = now()\n" +
            "WHERE id = :id;";

    @Override
    public void update(KabutoTask kabutoTask) {
        BeanPropertySqlParameterSource bs = new BeanPropertySqlParameterSource(kabutoTask);
        bs.registerSqlType("intervalUnit", Types.VARCHAR);
        bs.registerSqlType("status", Types.VARCHAR);
        namedParameterJdbcTemplate.update(UPDATE_SQL, bs);
    }

    final String CAS_UPDATE_SQL = "UPDATE kabuto_async_task\n" +
            "SET retry_count       = :retryCount,\n" +
            "    trigger_last_time = :triggerLastTime,\n" +
            "    trigger_next_time = :triggerNextTime,\n" +
            "    status            = :status,\n" +
            "    last_ip           = :lastIp,\n" +
            "    update_time       = now()\n" +
            "WHERE id = :id and trigger_next_time=:oldTriggerNextTime and status in ('INIT','EXECUTING','FAIL');";

    @Override
    public int casUpdate(KabutoTask kabutoTask, Long oldTriggerNextTime) {
        BeanMap beanMap = BeanMap.create(kabutoTask);
        Map<String, Object> params = new HashMap<>();
        params.putAll(beanMap);
        params.put("oldTriggerNextTime", oldTriggerNextTime);
        MapSqlParameterSource ms = new MapSqlParameterSource(params);
        ms.registerSqlType("intervalUnit", Types.VARCHAR);
        ms.registerSqlType("status", Types.VARCHAR);
        return namedParameterJdbcTemplate.update(CAS_UPDATE_SQL, ms);
    }
}
