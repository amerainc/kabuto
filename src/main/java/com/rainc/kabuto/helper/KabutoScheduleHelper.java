package com.rainc.kabuto.helper;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.rainc.kabuto.entity.KabutoTask;
import com.rainc.kabuto.enums.KabutoTaskStatusEnum;
import com.rainc.kabuto.repository.KabutoTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 监控任务调度
 *
 * @author rainc
 * @date 2022/5/20
 */
@Component
@Slf4j
public class KabutoScheduleHelper implements CommandLineRunner, DisposableBean {
    /**
     * 预读时常
     */
    public static final long PRE_READ_MS = 5000;
    private Thread scheduleThread;
    private Thread ringThread;
    private volatile boolean scheduleThreadToStop = false;
    private volatile boolean ringThreadToStop = false;
    private static Map<Integer, List<KabutoTask>> ringData = new ConcurrentHashMap<>();
    @Autowired
    KabutoTaskRepository kabutoTaskRepository;
    @Autowired
    KabutoTriggerHelper kabutoTriggerHelper;
    @Autowired
    KabutoContext kabutoContext;

    /**
     * 启动调度线程
     */
    private void scheduleThreadStart() {
        scheduleThread = new Thread(() -> {
            try {
                //睡眠保证精确到秒
                TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
            } catch (InterruptedException e) {
                if (!scheduleThreadToStop) {
                    log.error(e.getMessage(), e);
                }
            }
            // 预读数量
            int preReadCount = 1000;
            log.info("预读数量={}", preReadCount);
            log.info("初始化调度线程");
            while (!scheduleThreadToStop) {
                long nowTime = 0;
                boolean preSuc = false;
                try {
                    nowTime = System.currentTimeMillis();
                    preSuc = true;
                    //预读未来5秒内的触发任务
                    List<KabutoTask> kabutoTasks = kabutoTaskRepository.preloadAsyncTask(nowTime);
//                    log.info("预读任务列表:{}", JSON.toJSONString(kabutoTasks));
                    if (!CollectionUtils.isEmpty(kabutoTasks)) {
                        for (KabutoTask kabutoTask : kabutoTasks) {
                            try {
                                Long oldTriggerNextTime = kabutoTask.getTriggerNextTime();
                                if (nowTime > oldTriggerNextTime) {
                                    //如果已经到触发时间了，进行立即触发
                                    immediatelyTrigger(nowTime, kabutoTask, oldTriggerNextTime);
                                } else {
                                    //否则推入时间环进行触发
                                    push2RingTrigger(kabutoTask, oldTriggerNextTime);
                                }
                            } catch (Exception e) {
                                log.error("async trigger error :{}", kabutoTask, e);
                            }
                        }
                    } else {
                        preSuc = false;
                    }
                } catch (Exception e) {
                    if (!scheduleThreadToStop) {
                        log.error("MonitorScheduleHelper#scheduleThread error:", e);
                    }
                }

                //计算耗费时间
                long cost = System.currentTimeMillis() - nowTime;
                //如果小于1秒
                if (cost < 1000) {
                    try {
                        //预读成功则下一秒继续预读，预读失败则表示接下来5秒没有事件需要调度，睡眠5秒，减去%1000保证执行精确到秒
                        TimeUnit.MILLISECONDS.sleep((preSuc ? 1000 : PRE_READ_MS) - System.currentTimeMillis() % 1000);
                    } catch (InterruptedException e) {
                        if (!scheduleThreadToStop) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            }
            log.info("MonitorScheduleHelper#scheduleThread stop");
        });
        scheduleThread.setDaemon(true);
        scheduleThread.setName("MonitorScheduleHelper#scheduleThread");
        scheduleThread.start();
    }


    /**
     * 启动时间环线程
     */
    private void ringThreadStart() {
        ringThread = new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
            } catch (InterruptedException e) {
                if (!ringThreadToStop) {
                    log.error(e.getMessage(), e);
                }
            }

            while (!ringThreadToStop) {

                try {
                    //取得当前秒时间的数据
                    List<KabutoTask> ringItemData = new ArrayList<>();
                    int nowSecond = Calendar.getInstance().get(Calendar.SECOND);
                    // 避免处理耗时太长，跨过刻度，向前校验一个刻度；
                    for (int i = 0; i < 2; i++) {
                        List<KabutoTask> tmpData = ringData.remove((nowSecond + 60 - i) % 60);
                        if (tmpData != null) {
                            ringItemData.addAll(tmpData);
                        }
                    }

                    // 触发时间轮
                    log.debug("时间轮信息 : " + nowSecond + " = " + ringItemData);
                    if (ringItemData.size() > 0) {
                        //遍历当前时间的数据列表进行触发
                        ringItemData.forEach(kabutoTriggerHelper::trigger);
                        // 清空数据表
                        ringItemData.clear();
                    }
                } catch (Exception e) {
                    if (!ringThreadToStop) {
                        log.error("MonitorScheduleHelper#ringThread error:", e);
                    }
                }

                //睡眠到下一整秒再次执行
                try {
                    TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
                } catch (InterruptedException e) {
                    if (!ringThreadToStop) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
            log.info("MonitorScheduleHelper#ringThread stop");
        });
        ringThread.setDaemon(true);
        ringThread.setName("MonitorScheduleHelper#ringThread");
        ringThread.start();
    }

    /**
     * 立即触发
     *
     * @param nowTime
     * @param kabutoTask
     * @param oldTriggerNextTime
     * @return
     * @throws ParseException
     */
    private void immediatelyTrigger(long nowTime, KabutoTask kabutoTask, Long oldTriggerNextTime) throws ParseException {
        //计算新时间
        refreshNextValidTime(kabutoTask, new Date());
        KabutoTaskStatusEnum orignalStatus = kabutoTask.getStatus();
        int lockRet = casUpdate(kabutoTask, oldTriggerNextTime);
        //如果是5秒前的任务或者触发失败的任务，取消执行
        if (lockRet < 1) {
            return;
        }
        if (oldTriggerNextTime < nowTime - PRE_READ_MS){
            kabutoTask.setStatus(orignalStatus);
            kabutoTaskRepository.update(kabutoTask);
            return;
        }else {
            kabutoTriggerHelper.trigger(kabutoTask);
        }

        if (nowTime + PRE_READ_MS > kabutoTask.getTriggerNextTime()) {
            //如果刷新后在未来5秒内，则放入时间轮并刷新再次任务触发时间
            push2RingTrigger(kabutoTask, kabutoTask.getTriggerNextTime());
        }
    }

    /**
     * 推入时间环触发
     *
     * @param kabutoTask
     * @param oldTriggerNextTime
     * @throws ParseException
     */
    private void push2RingTrigger(KabutoTask kabutoTask, Long oldTriggerNextTime) throws ParseException {
        //创建新时间
        refreshNextValidTime(kabutoTask, new Date(oldTriggerNextTime));
        //尝试写入时间
        int lockRet = casUpdate(kabutoTask, oldTriggerNextTime);
        //写入成功则放入时间轮进行时间调度
        if (lockRet < 1) {
            return;
        }
        pushRing(kabutoTask, oldTriggerNextTime);
    }


    /**
     * 通过cas更新
     *
     * @param kabutoTask
     * @param oldTriggerNextTime
     * @return
     */
    private int casUpdate(KabutoTask kabutoTask, Long oldTriggerNextTime) {
        //是否有机器还在执行该任务
        boolean isRunningJob = KabutoTaskStatusEnum.EXECUTING.equals(kabutoTask.getStatus()) && kabutoContext.isActiveIp(kabutoTask.getLastIp());
        //如果不是本机执行，或者内存中的执行表里确定还在执行中
        boolean notSelfOrRunning = !kabutoContext.isCurrentIp(kabutoTask.getLastIp()) || kabutoContext.isRunning(kabutoTask.getId());
        if (isRunningJob && notSelfOrRunning) {
            //如果之前触发的不是本机，或者是本机但还在执行中
            kabutoTaskRepository.update(kabutoTask);
            return -1;
        }
        kabutoTask.setLastIp(kabutoContext.IP);
        kabutoTask.setStatus(KabutoTaskStatusEnum.EXECUTING);
        return kabutoTaskRepository.casUpdate(kabutoTask, oldTriggerNextTime);
    }

    /**
     * 更新下一次触发时间
     *
     * @param kabutoTask
     * @param fromTime
     * @throws ParseException
     */
    private void refreshNextValidTime(KabutoTask kabutoTask, Date fromTime) throws ParseException {
        String cron = kabutoTask.getCron();
        Date nextValidTime;
        if (StrUtil.isNotEmpty(cron)) {
            //计算下一次触发时间
            nextValidTime = new CronExpression(kabutoTask.getCron()).getNextValidTimeAfter(fromTime);
        } else {
            //计算下一次触发时间
            nextValidTime = new Date(fromTime.getTime() + kabutoTask.getIntervalUnit().toMillis(kabutoTask.getInterval()));
        }
        //计算成功，触发下一次
        kabutoTask.setTriggerLastTime(kabutoTask.getTriggerNextTime());
        kabutoTask.setTriggerNextTime(nextValidTime.getTime());
    }

    /**
     * 将任务放进时间轮
     *
     * @param kabutoTask   配置实例
     * @param triggerTime 触发时间
     */
    private void pushRing(KabutoTask kabutoTask, long triggerTime) {
        kabutoContext.addRunningConfig(kabutoTask.getId());
        // 1、计算放入的时间轮区域
        int ringSecond = (int) ((triggerTime / 1000) % 60);
        // 2、放入时间轮
        List<KabutoTask> ringItemData = ringData.computeIfAbsent(ringSecond, ArrayList::new);

        ringItemData.add(kabutoTask);
        log.debug("任务放入时间轮 : " + ringSecond + " = " + ringItemData);
    }


    @Override
    public void destroy() throws Exception {
        //中断两个线程
        stopScheduleThread();
        stopRingThread();
    }

    private void stopRingThread() {
        boolean hasRingData = false;
        for (List<KabutoTask> tmpData : ringData.values()) {
            if (!CollectionUtils.isEmpty(tmpData)) {
                hasRingData = true;
                break;
            }
        }
        //如果时间环里有数据，等待一段时间
        if (hasRingData) {
            try {
                TimeUnit.SECONDS.sleep(8);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
        //停止时间环线程
        ringThreadToStop = true;
        ringThread.interrupt();
        try {
            //阻塞直到线程结束
            ringThread.join();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void stopScheduleThread() {
        scheduleThreadToStop = true;
        scheduleThread.interrupt();
        try {
            //阻塞直到线程结束
            scheduleThread.join();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        scheduleThreadStart();
        ringThreadStart();
    }
}
