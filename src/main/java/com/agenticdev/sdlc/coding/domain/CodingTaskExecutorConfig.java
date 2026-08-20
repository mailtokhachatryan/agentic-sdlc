package com.agenticdev.sdlc.coding.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class CodingTaskExecutorConfig {

    /** Bounded executor for async coding runs so we can't drown the JVM under load. */
    @Bean(name = "codingTaskExecutor")
    TaskExecutor codingTaskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(32);
        ex.setThreadNamePrefix("coding-");
        ex.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
        ex.initialize();
        return ex;
    }
}
