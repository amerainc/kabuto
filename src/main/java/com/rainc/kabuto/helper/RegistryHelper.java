package com.rainc.kabuto.helper;

import com.rainc.kabuto.repository.RegistryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @Author rainc
 * g* @create 2020/12/12 12:48
 */
@Slf4j
@Component
public class RegistryHelper implements CommandLineRunner, DisposableBean {
    private Thread registryThread;
    private volatile boolean toStop = false;

    @Autowired
    RegistryRepository registryRepository;
    @Autowired
    KabutoContext kabutoContext;

    public void start() {
        registryThread = new Thread(() -> {
            log.info("kabuto注册");
            //注册检测
            while (!toStop) {
                try {
                    if (!registryRepository.hasRegistry(kabutoContext.IP)) {
                        registryRepository.init(kabutoContext.IP);
                    }else {
                        //心跳更新
                        registryRepository.beat(kabutoContext.IP);
                    }
                    //维护活跃表
                    registryRepository.maintainActive();
                } catch (Exception e) {
                    if (!toStop) {
                        log.error("心跳注册失败:{}",e.getMessage(), e);
                    }
                }

                //睡眠
                try {
                    if (!toStop) {
                        TimeUnit.SECONDS.sleep(30);
                    }
                } catch (InterruptedException e) {
                    if (!toStop) {
                        log.warn("执行器注册线程中断,错误信息:{}", e.getMessage());
                    }
                }
            }


            // 移除注册
            try {
                registryRepository.remove(kabutoContext.IP);
            } catch (Exception e) {
                if (!toStop) {
                    log.error(e.getMessage(), e);
                }
            }
            log.info("注册线程销毁");
        });

        registryThread.setDaemon(true);
        registryThread.setName("kabuto, executor ExecutorRegistryThread");
        registryThread.start();
    }
    public void toStop() {
        toStop = true;
        // interrupt and wait
        registryThread.interrupt();
        try {
            registryThread.join();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void destroy() throws Exception {
        toStop();
    }



    @Override
    public void run(String... args) throws Exception {
        start();
    }
}
