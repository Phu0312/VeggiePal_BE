package com.veggiepal.ai.video;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.groq.GroqClient;
import com.veggiepal.ai.groq.ModelProperties;
import com.veggiepal.ai.groq.dto.GroqRequest;
import com.veggiepal.ai.monitoring.UsageService;
import com.veggiepal.ai.quota.PremiumQuotaService;
import com.veggiepal.ai.video.VideoDtos.VideoJobResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoProcessingService {
    private final VideoJobRepository repository;private final GroqClient groq;private final ModelProperties models;private final ObjectMapper mapper;private final UsageService usage;private final PremiumQuotaService premiumQuota;
    private final String ffmpeg;private final String ffprobe;private final long maxBytes;private final long maxDurationSeconds;
    public VideoProcessingService(VideoJobRepository repository,GroqClient groq,ModelProperties models,ObjectMapper mapper,UsageService usage,PremiumQuotaService premiumQuota,
            @Value("${ai.video.ffmpeg-path}")String ffmpeg,@Value("${ai.video.ffprobe-path:ffprobe}")String ffprobe,
            @Value("${ai.video.max-file-bytes}")long maxBytes,@Value("${ai.video.max-duration-seconds:1800}")long maxDurationSeconds){
        this.repository=repository;this.groq=groq;this.models=models;this.mapper=mapper;this.usage=usage;this.premiumQuota=premiumQuota;this.ffmpeg=ffmpeg;this.ffprobe=ffprobe;
        this.maxBytes=maxBytes;this.maxDurationSeconds=maxDurationSeconds;}
    public VideoJobResponse process(Long userId,MultipartFile file){
        if(file.isEmpty()||file.getContentType()==null||!file.getContentType().toLowerCase().startsWith("video/"))throw new AppException(ErrorCode.INVALID_REQUEST);
        if(file.getSize()>maxBytes)throw new AppException(ErrorCode.FILE_TOO_LARGE);
        premiumQuota.reserveVideo(userId);
        VideoJob job=repository.save(new VideoJob(userId,safeName(file.getOriginalFilename())));Path video=null,audio=null;
        try{video=Files.createTempFile("veggiepal-video-",".upload");audio=Files.createTempFile("veggiepal-audio-",".mp3");file.transferTo(video);validateDuration(video);
            Process process=new ProcessBuilder(ffmpeg,"-y","-loglevel","error","-i",video.toString(),"-vn","-acodec","libmp3lame","-ar","16000","-ac","1",audio.toString()).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            if(!process.waitFor(120,TimeUnit.SECONDS)){process.destroyForcibly();throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED);}
            if(process.exitValue()!=0)throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED);
            usage.checkBudget("user:"+userId);long transcriptionStart=System.nanoTime();String transcript;
            try{transcript=groq.transcribe(audio,models.transcription());usage.success(userId,"user:"+userId,"TRANSCRIPTION",models.transcription(),
                    new GroqClient.GroqResult("",0,0),(System.nanoTime()-transcriptionStart)/1_000_000);}
            catch(RuntimeException exception){usage.failure(userId,"user:"+userId,"TRANSCRIPTION",models.transcription(),
                    (System.nanoTime()-transcriptionStart)/1_000_000,exception);throw exception;}
            usage.checkBudget("user:"+userId);long start=System.nanoTime();
            String prompt="Từ transcript video nấu ăn sau, trích xuất và trả duy nhất JSON {\"title\":\"\",\"ingredients\":[{\"name\":\"\",\"quantity\":\"\"}],\"steps\":[{\"number\":1,\"instruction\":\"\"}]}. Không bịa phần không nghe thấy. Transcript: "+transcript;
            GroqClient.GroqResult result;
            try{result=groq.complete(models.videoExtraction(),List.of(new GroqRequest.Message("user",prompt)),0.1,1200);
                usage.success(userId,"user:"+userId,"VIDEO",models.videoExtraction(),result,(System.nanoTime()-start)/1_000_000);}
            catch(RuntimeException exception){usage.failure(userId,"user:"+userId,"VIDEO",models.videoExtraction(),
                    (System.nanoTime()-start)/1_000_000,exception);throw exception;}
            JsonNode extraction=mapper.readTree(cleanJson(result.content()));validateExtraction(extraction);job.complete(transcript,extraction.toString());repository.save(job);return view(job);
        }catch(AppException e){job.fail(e.getMessage());repository.save(job);throw e;}catch(Exception e){job.fail(e.getMessage());repository.save(job);throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED,e);}finally{delete(video);delete(audio);}
    }
    public VideoJobResponse get(Long userId,Long id){return view(repository.findByIdAndUserId(id,userId).orElseThrow(()->new AppException(ErrorCode.RESOURCE_NOT_FOUND)));}
    private VideoJobResponse view(VideoJob j){try{return new VideoJobResponse(j.getId(),j.getStatus(),j.getOriginalFilename(),j.getTranscript(),j.getExtractionJson()==null?null:mapper.readValue(j.getExtractionJson(),Object.class),j.getErrorMessage(),j.getCreatedAt(),j.getUpdatedAt());}catch(Exception e){throw new AppException(ErrorCode.INVALID_AI_RESPONSE,e);}}
    private String safeName(String name){if(name==null)return"video";return Path.of(name).getFileName().toString().substring(0,Math.min(255,Path.of(name).getFileName().toString().length()));}
    private String cleanJson(String raw){return raw.replaceFirst("(?s)^```(?:json)?\\s*","").replaceFirst("\\s*```$","").trim();}
    private void validateDuration(Path video)throws Exception{
        Process process=new ProcessBuilder(ffprobe,"-v","error","-show_entries","format=duration","-of","default=noprint_wrappers=1:nokey=1",video.toString()).redirectErrorStream(true).start();
        if(!process.waitFor(20,TimeUnit.SECONDS)){process.destroyForcibly();throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED);}
        String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim();
        if(process.exitValue()!=0||output.isBlank())throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED);
        double duration=Double.parseDouble(output);if(!Double.isFinite(duration)||duration<=0||duration>maxDurationSeconds)throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED);
    }
    private void validateExtraction(JsonNode extraction){
        if(extraction==null||extraction.path("title").asText().isBlank()||!extraction.path("ingredients").isArray()
                ||extraction.path("ingredients").isEmpty()||!extraction.path("steps").isArray()||extraction.path("steps").isEmpty())
            throw new AppException(ErrorCode.INVALID_AI_RESPONSE);
        for(JsonNode ingredient:extraction.path("ingredients"))if(ingredient.path("name").asText().isBlank())
            throw new AppException(ErrorCode.INVALID_AI_RESPONSE);
        int expected=1;for(JsonNode step:extraction.path("steps")){
            if(step.path("number").asInt(-1)!=expected++||step.path("instruction").asText().isBlank())
                throw new AppException(ErrorCode.INVALID_AI_RESPONSE);
        }
    }
    private void delete(Path p){if(p!=null)try{Files.deleteIfExists(p);}catch(Exception ignored){}}
}
