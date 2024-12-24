package com.rainc.kabuto.config;

import com.rainc.kabuto.helper.KabutoTaskRunnable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @author rainc
 * @date 2023/4/12
 */
@Configuration
public class KabutoAutoConfig {
    @Bean("kabutoTaskExecutor")
    public Executor timingTaskExecutor(KabutoProperties kabutoProperties) {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(kabutoProperties.getFastPool());
        threadPoolTaskExecutor.setMaxPoolSize(kabutoProperties.getFastPool());
        threadPoolTaskExecutor.setQueueCapacity(1000);
        threadPoolTaskExecutor.setThreadFactory(new CustomizableThreadFactory("kabutoTaskExecutor"));
        threadPoolTaskExecutor.setRejectedExecutionHandler((r, executor) -> {
            if (r instanceof KabutoTaskRunnable) {
                ((KabutoTaskRunnable) r).rejected();
            }
        });
        return threadPoolTaskExecutor;
    }

    @Bean("kabutoTaskSlowExecutor")
    @ConditionalOnExpression("#kabutoProperties.slowPoolEnable")
    public Executor timingTaskSlowExecutor(KabutoProperties kabutoProperties) {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(kabutoProperties.getSlowPool());
        threadPoolTaskExecutor.setMaxPoolSize(kabutoProperties.getSlowPool());
        threadPoolTaskExecutor.setQueueCapacity(1000);
        threadPoolTaskExecutor.setThreadFactory(new CustomizableThreadFactory("kabutoTaskSlowExecutor"));
        threadPoolTaskExecutor.setRejectedExecutionHandler((r, executor) -> {
            if (r instanceof KabutoTaskRunnable) {
                ((KabutoTaskRunnable) r).rejected();
            }
        });
        return threadPoolTaskExecutor;
    }

    @Bean
    @ConfigurationProperties(prefix = "kabuto")
    public KabutoProperties kabutoProperties() {
        return new KabutoProperties();
    }

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class KabutoProperties {
        //快线程数
        Integer fastPool = 4;
        //慢线程数
        Integer slowPool=1;
        //慢线程时间（慢于这个时间认为是慢线程）单位秒
        Long slowTime=10L;
        /**
         * 是否是测试
         */
        boolean test = false;
        /**
         * 是有启用慢线程
         */
        boolean slowPoolEnable=false;
    }
}
