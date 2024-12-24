package com.rainc.kabuto.helper;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.rainc.kabuto.config.KabutoAutoConfig;
import com.rainc.kabuto.entity.KabutoTask;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * @author rainc
 * @date 2023/3/9
 */
@Slf4j
@Component
public class KabutoContext {
    @Autowired
    KabutoAutoConfig.KabutoProperties kabutoProperties;
    //--------------------活跃ip集合-----------------------
    private  final Set<String> ACTIVE_IP_SET = new ConcurrentHashSet<>();

    private  final Map<String, LinkedBlockingDeque<Long>> COST_TIME_MAP = new ConcurrentHashMap<>();

    public  Set<String> getActiveIpSet() {
        return ACTIVE_IP_SET;
    }
    public final String IP = NetUtil.getLocalhostStr();

    /**
     * 更新合集
     *
     * @param update
     */
    public  void updateSet(Collection<String> update) {
        synchronized (ACTIVE_IP_SET) {
            synchronized (ACTIVE_IP_SET) {
                ACTIVE_IP_SET.clear();
                ACTIVE_IP_SET.addAll(update);
            }
        }
    }

    /**
     * 检测是否是活跃ip
     *
     * @param ip
     * @return
     */
    public  boolean isActiveIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        return ACTIVE_IP_SET.contains(ip);
    }

    /**
     * 是否是当前ip
     *
     * @param ip
     * @return
     */
    public  boolean isCurrentIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        return IP.equals(ip);
    }

    //--------------------运行中的任务集合-----------------------
    private  final Set<Long> RUNNING_TASK = new ConcurrentHashSet<>();

    public  void addRunningConfig(Long taskId) {
        RUNNING_TASK.add(taskId);
    }

    public  void removeRunningConfig(Long taskId) {
        RUNNING_TASK.remove(taskId);
    }

    public  boolean isRunning(Long taskId) {
        return RUNNING_TASK.contains(taskId);
    }

    /**
     * 更新耗时
     *
     * @param cost
     */
    public void updateCostTime(KabutoTask kabutoTask, long cost) {
        LinkedBlockingDeque<Long> queue = COST_TIME_MAP.get(kabutoTask.getTaskType());
        if (queue == null) {
            synchronized (COST_TIME_MAP) {
                queue = COST_TIME_MAP.get(kabutoTask.getTaskType());
                if (queue == null) {
                    queue = LongStream.generate(() -> 0)
                            .limit(10)
                            .boxed()
                            .collect(Collectors.toCollection(() -> new LinkedBlockingDeque<>(10)));
                    COST_TIME_MAP.put(kabutoTask.getTaskType(), queue);
                }
            }
        }
        try {
            queue.take();
            queue.put(cost);
        } catch (InterruptedException e) {
            log.error("更新耗时失败", e);
        }
    }

    /**
     * 计算是否是慢任务
     */
    public boolean isSlow(KabutoTask kabutoTask) {
        LinkedBlockingDeque<Long> costTimes = COST_TIME_MAP.get(kabutoTask.getTaskType());
        if (costTimes == null) {
            return false;
        }
        long sum = 0;
        int count = 0;
        for (Long costTime : costTimes) {
            if (costTime == 0) {
                continue;
            }
            sum += costTime;
            count++;
        }
        long avgCostTime = count > 0 ? sum / count : 0;
        //超过10秒的放入慢线程池，其他放入快线程池
        return avgCostTime > TimeUnit.SECONDS.toMillis(kabutoProperties.getSlowTime());
    }
}
