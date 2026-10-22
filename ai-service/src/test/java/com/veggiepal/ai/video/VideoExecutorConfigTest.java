package com.veggiepal.ai.video;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class VideoExecutorConfigTest {
    @Test
    void executorRejectsWorkBeyondBoundedWorkerAndQueueCapacity() throws Exception {
        VideoProperties properties = new VideoProperties("ffmpeg", "ffprobe", 100, 1800,
                Duration.ofMinutes(2), Duration.ofSeconds(20), 1, 1, "build/video",
                Duration.ofMinutes(20), Duration.ofDays(30), Duration.ofMinutes(5));
        Executor executor = new VideoExecutorConfig().videoTaskExecutor(properties);
        CountDownLatch release = new CountDownLatch(1);
        Runnable blocked = () -> {
            try { release.await(); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
        };
        try {
            executor.execute(blocked);
            executor.execute(blocked);
            assertThrows(TaskRejectedException.class, () -> executor.execute(blocked));
        } finally {
            release.countDown();
            ((ThreadPoolTaskExecutor) executor).shutdown();
        }
    }
}
