package com.veggiepal.ai.video;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class VideoMediaTools {
    private final VideoProperties properties;
    private final ObjectMapper mapper;

    public VideoMediaTools(VideoProperties properties, ObjectMapper mapper) {
        this.properties = properties; this.mapper = mapper;
    }

    public Probe probe(Path video) {
        Process process = null;
        try {
            process = new ProcessBuilder(properties.ffprobePath(), "-v", "error", "-select_streams", "v:0",
                    "-show_entries", "stream=codec_type:format=duration,format_name", "-of", "json",
                    video.toString()).redirectErrorStream(true).start();
            if (!process.waitFor(properties.ffprobeTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED);
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) throw new AppException(ErrorCode.INVALID_REQUEST);
            var json = mapper.readTree(output);
            double duration = json.path("format").path("duration").asDouble(-1);
            String format = json.path("format").path("format_name").asText("");
            boolean videoStream = json.path("streams").isArray() && !json.path("streams").isEmpty()
                    && "video".equals(json.path("streams").get(0).path("codec_type").asText());
            if (!videoStream || format.isBlank() || !Double.isFinite(duration) || duration <= 0
                    || duration > properties.maxDurationSeconds()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            return new Probe(duration, format);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.INVALID_REQUEST, exception);
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
    }

    public void extractAudio(Path video, Path audio) {
        Process process = null;
        try {
            process = new ProcessBuilder(properties.ffmpegPath(), "-y", "-loglevel", "error", "-i",
                    video.toString(), "-vn", "-acodec", "libmp3lame", "-ar", "16000", "-ac", "1",
                    audio.toString()).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            if (!process.waitFor(properties.ffmpegTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED);
            }
            if (process.exitValue() != 0) throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED, exception);
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
    }

    public record Probe(double durationSeconds, String formatName) { }
}
