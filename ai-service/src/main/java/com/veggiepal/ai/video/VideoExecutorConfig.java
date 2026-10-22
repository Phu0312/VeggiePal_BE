package com.veggiepal.ai.video;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class VideoExecutorConfig {
    @Bean("videoTaskExecutor")
    Executor videoTaskExecutor(VideoProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(1, properties.workerCount()));
        executor.setMaxPoolSize(Math.max(1, properties.workerCount()));
        executor.setQueueCapacity(Math.max(0, properties.queueCapacity()));
        executor.setThreadNamePrefix("video-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
