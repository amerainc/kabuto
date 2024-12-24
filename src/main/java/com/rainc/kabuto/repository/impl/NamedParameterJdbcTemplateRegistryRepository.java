package com.rainc.kabuto.repository.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.rainc.kabuto.repository.RegistryRepository;
import com.rainc.kabuto.helper.KabutoContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import java.sql.ResultSet;
import java.util.*;

/**
 * @author rainc
 * @date 2023/4/10
 */
@Component
public class NamedParameterJdbcTemplateRegistryRepository implements RegistryRepository {
    @Autowired
    KabutoContext kabutoContext;
    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    final String HAS_REGISTRY = "select 1 from kabuto_registry where ip=:ip;";

    @Override
    public boolean hasRegistry(String ip) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("ip", ip);
       return namedParameterJdbcTemplate.query(HAS_REGISTRY, params,ResultSet::next);
    }

    final String INIT = "insert into kabuto_registry(ip,active,update_time) values(:ip,true,now());";

    @Override
    public void init(String ip) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("ip", ip);
        namedParameterJdbcTemplate.update(INIT, params);
    }

    final String BEAT = "update kabuto_registry set update_time=now(),active=true where ip=:ip;";

    @Override
    public void beat(String ip) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("ip", ip);
        namedParameterJdbcTemplate.update(BEAT, params);
    }

    final static String ACTIVE_IP = "select ip from kabuto_registry where active=true";

    @Override
    public void maintainActive() {
        //删除数据库过期数据
        deleteDead();
        List<String> ipList = namedParameterJdbcTemplate.queryForList(ACTIVE_IP,new HashMap<>(), String.class);
        kabutoContext.updateSet(ipList);
    }

    final static String REMOVE = "update kabuto_registry set active=false where ip=:ip";

    @Override
    public void remove(String ip) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("ip", ip);
        namedParameterJdbcTemplate.update(REMOVE, params);
    }

    final static String DELETE_DEAD = "update kabuto_registry set active=false where active=true and update_time<:deadTime;";

    /**
     * 删除过期数据
     */
    private void deleteDead() {
        Date now = new Date();
        DateTime deadTime = DateUtil.offsetSecond(now, -30);
        HashMap<String, Object> params = new HashMap<>();
        params.put("deadTime", deadTime);
        namedParameterJdbcTemplate.update(DELETE_DEAD, params);
    }
}
