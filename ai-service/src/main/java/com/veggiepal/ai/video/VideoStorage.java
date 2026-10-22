package com.veggiepal.ai.video;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class VideoStorage {
    private final Path root;
    private final Semaphore capacity;
    private final Set<Path> owned = ConcurrentHashMap.newKeySet();

    public VideoStorage(VideoProperties properties) {
        String configured = properties.storageDir();
        if (configured == null || configured.isBlank()) {
            configured = Path.of(System.getProperty("java.io.tmpdir"), "veggiepal-ai-video").toString();
        }
        this.root = Path.of(configured).toAbsolutePath().normalize();
        this.capacity = new Semaphore(properties.storageCapacity());
    }

    @PostConstruct
    void initialize() throws java.io.IOException { Files.createDirectories(root); }

    public Path store(MultipartFile file) {
        if (!capacity.tryAcquire()) throw new AppException(ErrorCode.VIDEO_QUEUE_FULL);
        Path target = root.resolve(UUID.randomUUID() + ".video").normalize();
        try {
            file.transferTo(target);
            owned.add(target);
            return target;
        } catch (Exception exception) {
            capacity.release();
            deleteFile(target);
            throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED, exception);
        }
    }

    public Path createAudio() throws java.io.IOException {
        Path audio = root.resolve(UUID.randomUUID() + ".mp3").normalize();
        Files.createFile(audio);
        return audio;
    }

    public void delete(Path path) {
        if (path == null) return;
        Path safe = path.toAbsolutePath().normalize();
        if (!safe.startsWith(root)) return;
        deleteFile(safe);
        if (owned.remove(safe)) capacity.release();
    }

    public void cleanupOrphans(Duration olderThan) {
        Instant cutoff = Instant.now().minus(olderThan);
        try (var files = Files.list(root)) {
            files.filter(path -> !owned.contains(path)).filter(path -> {
                try { return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff); }
                catch (Exception ignored) { return false; }
            }).forEach(this::delete);
        } catch (Exception ignored) { }
    }

    private void deleteFile(Path path) {
        try { Files.deleteIfExists(path); } catch (Exception ignored) { }
    }
}
